package org.commcare.cases.instance;

import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.QuerySensitive;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;

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
