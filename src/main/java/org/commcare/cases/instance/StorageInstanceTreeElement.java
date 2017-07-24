package org.commcare.cases.instance;

import org.commcare.cases.query.QueryContext;
import org.commcare.cases.util.StorageBackedTreeRoot;
import org.commcare.modern.engine.cases.RecordObjectCache;
import org.commcare.modern.engine.cases.RecordSetResultCache;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.Interner;
import org.javarosa.core.util.externalizable.Externalizable;

import java.util.LinkedHashSet;
import java.util.Vector;

/**
 * Instance root for storage-backed instances such as the case and ledger DBs
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public abstract class StorageInstanceTreeElement<Model extends Externalizable, T extends AbstractTreeElement>
        extends StorageBackedTreeRoot<T> {

    private String modelName;
    private String childName;

    private AbstractTreeElement instanceRoot;

    protected final IStorageUtilityIndexed<Model> storage;
    protected Vector<T> elements;
    protected final Interner<TreeElement> treeCache = new Interner<>();
    private Interner<String> stringCache = new Interner<>();

    private int numRecords = -1;
    private TreeReference cachedRef = null;

    public StorageInstanceTreeElement(AbstractTreeElement instanceRoot,
                                      IStorageUtilityIndexed<Model> storage,
                                      String modelName, String childName) {
        this.instanceRoot = instanceRoot;
        this.storage = storage;
        this.modelName = modelName;
        this.childName = childName;
    }

    /**
     * Rebase assigns this tree element to a new root instance node.
     *
     * Used to migrate the already created tree structure to a new instance connector.
     *
     * @param instanceRoot The root of the new tree that this element should be a part of
     */
    public void rebase(AbstractTreeElement instanceRoot) {
        this.instanceRoot = instanceRoot;
        expireCachedRef();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean isChildable() {
        return false;
    }

    @Override
    public String getInstanceName() {
        return instanceRoot.getInstanceName();
    }

    @Override
    public T getChild(String name, int multiplicity) {
        if ((multiplicity == TreeReference.INDEX_TEMPLATE) &&
                childName.equals(name)) {
            return getChildTemplate();
        }

        //name is always "case", so multiplicities are the only relevant component here
        if (childName.equals(name)) {
            loadElements();
            if (elements.isEmpty()) {
                //If we have no cases, we still need to be able to return a template element so as to not
                //break xpath evaluation
                return getChildTemplate();
            }
            return elements.elementAt(multiplicity);
        }
        return null;
    }

    protected synchronized void loadElements() {
        if (elements != null) {
            return;
        }
        elements = new Vector<>();
        int mult = 0;
        for (IStorageIterator i = storage.iterate(); i.hasMore(); ) {
            int id = i.nextID();
            elements.add(buildElement(this, id, null, mult));
            objectIdMapping.put(DataUtil.integer(id), DataUtil.integer(mult));
            mult++;
        }
    }

    @Override
    public Vector<T> getChildrenWithName(String name) {
        if (name.equals(childName)) {
            loadElements();
            return elements;
        } else {
            return new Vector<>();
        }
    }

    @Override
    public boolean hasChildren() {
        return getNumChildren() > 0;
    }

    @Override
    public int getNumChildren() {
        if (numRecords == -1) {
            numRecords = storage.getNumRecords();
        }
        return numRecords;
    }

    @Override
    public T getChildAt(int i) {
        loadElements();
        return elements.elementAt(i);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isAttribute() {
        return false;
    }

    @Override
    public int getChildMultiplicity(String name) {
        //All children have the same name;
        if (name.equals(childName)) {
            return this.getNumChildren();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isRelevant() {
        return true;
    }

    @Override
    public void accept(ITreeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int getAttributeCount() {
        return 0;
    }

    @Override
    public String getAttributeNamespace(int index) {
        return null;
    }

    @Override
    public String getAttributeName(int index) {
        return null;
    }

    @Override
    public String getAttributeValue(int index) {
        return null;
    }

    @Override
    public T getAttribute(String namespace, String name) {
        return null;
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        return null;
    }

    @Override
    public TreeReference getRef() {
        if (cachedRef == null) {
            cachedRef = TreeReference.buildRefFromTreeElement(this);
        }
        return cachedRef;
    }

    private void expireCachedRef() {
        cachedRef = null;
    }

    @Override
    public String getName() {
        return modelName;
    }

    @Override
    public int getMult() {
        return 0;
    }

    @Override
    public AbstractTreeElement getParent() {
        return instanceRoot;
    }

    @Override
    public IAnswerData getValue() {
        return null;
    }

    @Override
    public int getDataType() {
        return 0;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    protected String getChildHintName() {
        return childName;
    }

    @Override
    protected IStorageUtilityIndexed<?> getStorage() {
        return storage;
    }

    @Override
    protected void initStorageCache() {
        loadElements();
    }

    public void attachStringCache(Interner<String> stringCache) {
        this.stringCache = stringCache;
    }

    public String intern(String s) {
        if (stringCache == null) {
            return s;
        } else {
            return stringCache.intern(s);
        }
    }

    protected abstract T buildElement(StorageInstanceTreeElement<Model, T> storageInstance,
                                      int recordId, String id, int mult);

    protected Model getElement(int recordId, QueryContext context) {
        if (context == null || getStorageCacheName() == null) {
            return getElementSingular(recordId, context);
        }
        RecordSetResultCache recordSetCache = context.getQueryCacheOrNull(RecordSetResultCache.class);

        RecordObjectCache<Model> recordObjectCache = getRecordObjectCacheIfRelevant(context);

        if(recordObjectCache != null) {
            if (recordObjectCache.isLoaded(recordId)) {
                return recordObjectCache.getLoadedRecordObject(recordId);
            }

            if (canLoadRecordFromGroup(recordSetCache, recordId)) {
                Pair<String, LinkedHashSet<Integer>> tranche =
                        recordSetCache.getRecordSetForRecordId(getStorageCacheName(), recordId);
                EvaluationTrace loadTrace =
                        new EvaluationTrace(String.format("Model [%s]: Bulk Load [%s}",
                                this.getStorageCacheName(),tranche.first));

                LinkedHashSet<Integer>  body = tranche.second;
                storage.bulkRead(body, recordObjectCache.getLoadedCaseMap());
                loadTrace.setOutcome("Loaded: " + body.size());
                context.reportTrace(loadTrace);

                return recordObjectCache.getLoadedRecordObject(recordId);
            }
        }

        return getElementSingular(recordId, context);
    }

    private boolean canLoadRecordFromGroup(RecordSetResultCache recordSetCache, int recordId) {
        return recordSetCache != null && recordSetCache.hasMatchingRecordSet(getStorageCacheName(), recordId);
    }

    /**
     * Get a record object cache if it's appropriate in the current context.
     */
    private RecordObjectCache getRecordObjectCacheIfRelevant(QueryContext context) {
        // If the query isn't currently in a bulk mode, don't force an object cache to exist unless
        // it already does
        if (context.getScope() < QueryContext.BULK_QUERY_THRESHOLD) {
            return context.getQueryCacheOrNull(RecordObjectCache.class);
        } else {
            return context.getQueryCache(RecordObjectCache.class);
        }
    }

    /**
     * Retrieves a model for the provided record ID using a guaranteed singular lookup from
     * storage. This is the "Safe" fallback behavior for lookups.
     */
    protected Model getElementSingular(int recordId, QueryContext context) {
        EvaluationTrace trace = new EvaluationTrace(String.format("Model [%s]: Singular Load", getStorageCacheName()));

        Model m = storage.read(recordId);

        trace.setOutcome(String.valueOf(recordId));
        if(context!= null) {
            context.reportTrace(trace);
        }
        return m;
    }

    protected Model getModelTemplate() {
        return storage.read(1);
    }

    protected abstract T getChildTemplate();
}
