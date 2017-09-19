package org.commcare.cases.instance;

import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.QuerySensitive;
import org.commcare.cases.query.ScopeLimitedReferenceRequestCache;
import org.commcare.modern.engine.cases.RecordObjectCache;
import org.commcare.modern.engine.cases.RecordSetResultCache;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.model.trace.EvaluationTraceReduction;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Child TreeElement of an indexed fixture whose data is loaded from a DB.
 *
 * i.e. 'product' of "instance('product-list')/products/product"
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class IndexedFixtureChildElement extends StorageBackedChildElement<StorageIndexedTreeElementModel> implements QuerySensitive{
    private TreeElement empty;

    protected IndexedFixtureChildElement(StorageInstanceTreeElement<StorageIndexedTreeElementModel, ?> parent,
                                         int mult, int recordId) {
        super(parent, mult, recordId, parent.getName(), parent.getChildHintName());
    }

    @Override
    protected TreeElement cache(QueryContext context) {
        if (recordId == TreeReference.INDEX_TEMPLATE) {
            return empty;
        }

        synchronized (parent.treeCache) {
            TreeElement partialMatch = detectAndProcessLimitedScopeResponse(recordId,context);
            if(partialMatch != null) {
                return partialMatch;
            }

            TreeElement element = parent.treeCache.retrieve(recordId);
            if (element != null) {
                return element;
            }

            StorageIndexedTreeElementModel model = parent.getElement(recordId, context);
            TreeElement cacheBuilder = buildElementFromModel(model);

            parent.treeCache.register(recordId, cacheBuilder);

            return cacheBuilder;
        }
    }

    /**
     * Identifies whether in the current context it is potentially the case that a "partial"
     * response is acceptable, and builds the response if so.
     *
     * Returns null if that strategy is not applicable or if a partial response could not
     * be generated
     */
    private TreeElement detectAndProcessLimitedScopeResponse(int recordId, QueryContext context) {
        if(context == null) {
            return null;
        }
        ScopeLimitedReferenceRequestCache cache =
                context.getQueryCacheOrNull(ScopeLimitedReferenceRequestCache.class);

        if(cache == null) {
            return null;
        }

        //If cache already contains partial match, return it here...
        TreeElement partialMatch = cache.getCachedElementIfExists(this.getInstanceName(), recordId);
        if(partialMatch != null) {
            return partialMatch;
        }

        if(!cache.isInstancePotentiallyScopeLimited(this.getInstanceName())) {
            return null;
        }

        String[] scopeSufficientColumnList = analyseScopeSufficientColumnList(cache);

        if(scopeSufficientColumnList == null) {
            return null;
        }

        String[] objectMetadata = getElementMetadata(recordId, scopeSufficientColumnList, context);

        partialMatch = this.buildPartialElementFromMetadata(scopeSufficientColumnList, objectMetadata);
        cache.cacheElement(this.getInstanceName(), recordId, partialMatch);
        return partialMatch;
    }

    protected String[] getElementMetadata(int recordId, String[] metaFields, QueryContext context) {
        if (context == null || this.parent.getStorageCacheName() == null) {
            return parent.storage.getMetaDataForRecord(recordId, metaFields);
        }
        RecordSetResultCache recordSetCache = context.getQueryCacheOrNull(RecordSetResultCache.class);

        String recordSetKey = parent.getStorageCacheName();

        String recordObjectKey = parent.getStorageCacheName() + "_partial";

        RecordObjectCache<String[]> recordObjectCache = StorageInstanceTreeElement.getRecordObjectCacheIfRelevant(
                context);

        if(recordObjectCache != null) {
            if (recordObjectCache.isLoaded(recordObjectKey, recordId)) {
                return recordObjectCache.getLoadedRecordObject(recordObjectKey, recordId);
            }

            if (StorageInstanceTreeElement.canLoadRecordFromGroup(recordSetCache, recordSetKey, recordId)) {
                Pair<String, LinkedHashSet<Integer>> tranche =
                        recordSetCache.getRecordSetForRecordId(recordSetKey, recordId);

                EvaluationTrace loadTrace =
                        new EvaluationTrace(String.format("Model [%s]: Limited Scope Partial Bulk Load [%s}",
                                recordObjectKey,tranche.first));

                LinkedHashSet<Integer>  body = tranche.second;
                parent.getStorage().bulkReadMetadata(body, metaFields, recordObjectCache.getLoadedCaseMap(recordObjectKey));
                loadTrace.setOutcome("Loaded: " + body.size());

                context.reportTrace(loadTrace);

                return recordObjectCache.getLoadedRecordObject(recordObjectKey, recordId);
            }
        }

        return parent.storage.getMetaDataForRecord(recordId, metaFields);
    }

    private String[] analyseScopeSufficientColumnList(ScopeLimitedReferenceRequestCache cache) {
        String[] limitedScope = cache.getInternalScopedLimit(this.getInstanceName());
        if(limitedScope != null) {
            return limitedScope;
        }

        //If we don't already have that list, build it (or detect that it's won't be possible and tell
        //the cache to not try.

        Set<TreeReference> referencesInScope =
                cache.getLimitedReferenceSet(this.getInstanceName());

        StorageIndexedTreeElementModel model = parent.getModelTemplate();

        //TODO: Make sure this doesn't recursively call cache...
        TreeReference baseRefForChildElement = this.getRef().genericize();

        Vector<String> relativeSteps = model.getIndexedTreeReferenceSteps();
        HashMap<TreeReference, String> stepToColumnName = new HashMap<>();

        for(String relativeStep : relativeSteps) {
            stepToColumnName.put(XPathReference.getPathExpr(relativeStep).getReference(),
                    StorageIndexedTreeElementModel.getSqlColumnNameFromElementOrAttribute(relativeStep));
        }

        boolean failed = false;

        HashSet<String> columnNameCacheLoads = new HashSet<>();

        for(TreeReference inScopeReference : referencesInScope) {
            //raw references to this node are expected if predicates exist, and don't require
            //specific reads
            if(inScopeReference.equals(baseRefForChildElement)) {
                continue;
            }

            TreeReference subReference = inScopeReference.relativize(baseRefForChildElement);
            if(!stepToColumnName.containsKey(subReference)){
                failed = true;
                break;
            } else {
                columnNameCacheLoads.add(stepToColumnName.get(subReference));
            }
        }

        if(failed) {
            cache.setScopeLimitUnhelpful(this.getInstanceName());
            return null;
        }

        String[] columnList = new String[columnNameCacheLoads.size()];
        int i = 0;
        for(String s : columnNameCacheLoads) {
            columnList[i] = s;
            i++;
        }

        cache.setInternalScopeLimit(this.getInstanceName(), columnList);

        return columnList;
    }

    private TreeElement buildPartialElementFromMetadata(String[] columnNames, String[] metadataValues) {
        TreeElement partial = new TreeElement(parent.getChildHintName());
        partial.setMult(mult);
        partial.setParent(this.parent);

        for(int i =0 ; i < columnNames.length; ++i) {
            String columnName = columnNames[i];
            String value = metadataValues[i];

            String metadataName = StorageIndexedTreeElementModel.
                    getElementOrAttributeFromSqlColumnName(columnName);

            if(metadataName.startsWith("@")) {
                partial.setAttribute(null, metadataName.substring(1), value);
            } else {
                TreeElement child = new TreeElement(metadataName);
                child.setValue(new UncastData(value));
                partial.addChild(child);
            }
        }
        return partial;
    }

    private TreeElement buildElementFromModel(StorageIndexedTreeElementModel model) {
        TreeElement cacheBuilder = model.getRoot();
        entityId = model.getEntityId();
        cacheBuilder.setMult(mult);
        cacheBuilder.setParent(this.parent);

        return cacheBuilder;
    }

    @Override
    public String getName() {
        return nameId;
    }

    public static IndexedFixtureChildElement buildFixtureChildTemplate(IndexedFixtureInstanceTreeElement parent) {
        IndexedFixtureChildElement template =
                new IndexedFixtureChildElement(parent, TreeReference.INDEX_TEMPLATE, TreeReference.INDEX_TEMPLATE);

        StorageIndexedTreeElementModel modelTemplate = parent.getModelTemplate();
        // NOTE PLM: do we need to do more to convert a regular TreeElement into a template?
        template.empty = modelTemplate.getRoot();
        template.empty.setMult(TreeReference.INDEX_TEMPLATE);
        return template;
    }

    @Override
    public void prepareForUseInCurrentContext(QueryContext queryContext) {
        cache(queryContext);
    }
}
