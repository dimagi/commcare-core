package org.commcare.cases.instance;

import org.commcare.cases.model.StorageBackedModel;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;

import java.util.Hashtable;
import java.util.Set;

/**
 * Child TreeElement of a flat fixture whose data is loaded from a DB.
 *
 * i.e. 'product' of "instance('product-list')/products/product"
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FlatFixtureChildElement extends StorageBackedChildElement<StorageBackedModel> {
    private TreeElement empty;

    protected FlatFixtureChildElement(StorageInstanceTreeElement<StorageBackedModel, ?> parent,
                                      int mult, int recordId) {
        super(parent, mult, recordId, parent.getName(), parent.getChildHintName());
    }

    /**
     * Template constructor (For elements that need to create reference nodesets but never look up values)
     */
    private FlatFixtureChildElement(StorageInstanceTreeElement<StorageBackedModel, ?> parent) {
        super(parent, TreeReference.INDEX_TEMPLATE,
                TreeReference.INDEX_TEMPLATE, parent.getName(),
                parent.getChildHintName());

        empty = new TreeElement(nameId);
        empty.setMult(this.mult);
        empty.setAttribute(null, nameId, "");

        StorageBackedModel modelTemplate = parent.getModelTemplate();

        addBlankAttributes(empty, modelTemplate.getAttributes().keySet());
        addBlankElements(empty, modelTemplate.getElements().keySet());
    }

    private static void addBlankAttributes(TreeElement template,
                                           Set<String> attributeKeys) {
        for (String key : attributeKeys) {
            template.setAttribute(null, key, "");
        }
    }

    private static void addBlankElements(TreeElement template,
                                         Set<String> elementKeys) {
        for (String key : elementKeys) {
            TreeElement scratch = new TreeElement(key);
            scratch.setAnswer(null);
            template.addChild(scratch);
        }
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

            StorageBackedModel model = parent.getElement(recordId);
            TreeElement cacheBuilder = buildElementFromModel(model);

            parent.treeCache.register(recordId, cacheBuilder);

            return cacheBuilder;
        }
    }

    private TreeElement buildElementFromModel(StorageBackedModel model) {
        TreeElement cacheBuilder = new TreeElement(nameId);
        entityId = model.getEntityId();
        cacheBuilder.setMult(mult);

        addAttributes(cacheBuilder, model.getAttributes());
        addElements(cacheBuilder, model.getElements());
        addNestedElements(cacheBuilder, model.getNestedElements());
        cacheBuilder.setParent(this.parent);

        return cacheBuilder;
    }

    private static void addAttributes(TreeElement treeElement,
                                      Hashtable<String, String> attributes) {
        for (String key : attributes.keySet()) {
            treeElement.setAttribute(null, key, attributes.get(key));
        }
    }
    private static void addElements(TreeElement treeElement,
                                    Hashtable<String, String> elements) {
        for (String key : elements.keySet()) {
            TreeElement scratch = new TreeElement(key);
            String data = elements.get(key);
            // TODO PLM: do we want smarter type dispatch?
            scratch.setAnswer(new StringData(data == null ? "" : data));
            treeElement.addChild(scratch);
        }
    }

    private static void addNestedElements(TreeElement treeElement,
                                          Hashtable<String, String> nestedElements) {
        for (String key : nestedElements.keySet()) {
            String[] segments = key.split("/");
            TreeElement child = treeElement.getChild(segments[0], 0);
            TreeElement scratch = new TreeElement(segments[1]);
            String data = nestedElements.get(key);
            // TODO PLM: do we want smarter type dispatch?
            scratch.setAnswer(new StringData(data == null ? "" : data));
            child.addChild(scratch);
        }
    }

    @Override
    public String getName() {
        return nameId;
    }

    public static FlatFixtureChildElement buildFixtureChildTemplate(FlatFixtureInstanceTreeElement parent) {
        return new FlatFixtureChildElement(parent);
    }
}
