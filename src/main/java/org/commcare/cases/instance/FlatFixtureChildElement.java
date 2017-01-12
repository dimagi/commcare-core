package org.commcare.cases.instance;

import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;

/**
 * Child TreeElement of a flat fixture whose data is loaded from a DB.
 *
 * i.e. 'product' of "instance('product-list')/products/product"
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FlatFixtureChildElement extends StorageBackedChildElement<StorageIndexedTreeElementModel> {
    private TreeElement empty;

    protected FlatFixtureChildElement(StorageInstanceTreeElement<StorageIndexedTreeElementModel, ?> parent,
                                      int mult, int recordId) {
        super(parent, mult, recordId, parent.getName(), parent.getChildHintName());
    }

    /**
     * Template constructor (For elements that need to create reference nodesets but never look up values)
     */
    private FlatFixtureChildElement(StorageInstanceTreeElement<StorageIndexedTreeElementModel, ?> parent) {
        super(parent, TreeReference.INDEX_TEMPLATE,
                TreeReference.INDEX_TEMPLATE, parent.getName(),
                parent.getChildHintName());

        StorageIndexedTreeElementModel modelTemplate = parent.getModelTemplate();
        empty = modelTemplate.getRoot();
        empty.setMult(TreeReference.INDEX_TEMPLATE);
        // NOTE PLM: do we need to do more to convert a regular TreeElement into a template?
    }

    @Override
    protected TreeElement cache() {
        if (recordId == TreeReference.INDEX_TEMPLATE) {
            return empty;
        }

        synchronized (parent.treeCache) {
            TreeElement element = parent.treeCache.retrieve(recordId);
            if (element != null) {
                return element;
            }

            StorageIndexedTreeElementModel model = parent.getElement(recordId);
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

    public static FlatFixtureChildElement buildFixtureChildTemplate(FlatFixtureInstanceTreeElement parent) {
        return new FlatFixtureChildElement(parent);
    }
}
