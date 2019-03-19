package org.commcare.cases.instance;

import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.commcare.core.interfaces.UserSandbox;
import org.javarosa.core.model.IndexedFixtureIndex;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.InstanceBase;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Hashtable;

/**
 * The root element for the an indexed fixture data instance:
 * instance('some-indexed-fixture')/fixture-root
 * All children are nodes in a database table associated with the fixture.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class IndexedFixtureInstanceTreeElement
        extends StorageInstanceTreeElement<StorageIndexedTreeElementModel, IndexedFixtureChildElement> {

    private Hashtable<XPathPathExpr, String> storageIndexMap = null;
    private String cacheKey;
    private TreeElement attrHolder;

    private IndexedFixtureInstanceTreeElement(AbstractTreeElement instanceRoot,
                                              IStorageUtilityIndexed<StorageIndexedTreeElementModel> storage,
                                              IndexedFixtureIndex indexedFixtureIndex) {
        super(instanceRoot, storage, indexedFixtureIndex.getBase(), indexedFixtureIndex.getChild());
        attrHolder = indexedFixtureIndex.getAttrs();
        cacheKey = indexedFixtureIndex.getBase() + "|" + indexedFixtureIndex.getChild();
    }

    public static IndexedFixtureInstanceTreeElement get(UserSandbox sandbox,
                                                        String instanceName,
                                                        InstanceBase instanceBase) {
        IndexedFixtureIndex indexedFixtureIndex =
                sandbox.getIndexedFixturePathBases(instanceName);
        if (indexedFixtureIndex == null) {
            return null;
        } else {
            IStorageUtilityIndexed<StorageIndexedTreeElementModel> storage =
                    sandbox.getIndexedFixtureStorage(instanceName);
            return new IndexedFixtureInstanceTreeElement(instanceBase, storage, indexedFixtureIndex);
        }
    }

    @Override
    protected IndexedFixtureChildElement buildElement(StorageInstanceTreeElement<StorageIndexedTreeElementModel, IndexedFixtureChildElement> storageInstance,
                                                      int recordId, String id, int mult) {
        return new IndexedFixtureChildElement(storageInstance, mult, recordId);
    }

    @Override
    protected IndexedFixtureChildElement getChildTemplate() {
        return IndexedFixtureChildElement.buildFixtureChildTemplate(this);
    }

    @Override
    protected Hashtable<XPathPathExpr, String> getStorageIndexMap() {
        if (storageIndexMap == null) {
            storageIndexMap = new Hashtable<>();

            StorageIndexedTreeElementModel template = getModelTemplate();
            for (String fieldName : template.getMetaDataFields()) {
                String entry = StorageIndexedTreeElementModel.getElementOrAttributeFromSqlColumnName(fieldName);
                storageIndexMap.put(XPathReference.getPathExpr(entry), fieldName);
            }
        }

        return storageIndexMap;
    }

    public String getStorageCacheName() {
        return cacheKey;
    }

    @Override
    public int getAttributeCount() {
        return attrHolder.getAttributeCount();
    }

    @Override
    public String getAttributeNamespace(int index) {
        return attrHolder.getAttributeNamespace(index);
    }

    @Override
    public String getAttributeName(int index) {
        return attrHolder.getAttributeName(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return attrHolder.getAttributeValue(index);
    }

    @Override
    public AbstractTreeElement getAttribute(String namespace, String name) {
        TreeElement attr = attrHolder.getAttribute(namespace, name);
        attr.setParent(this);
        return attr;
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        return attrHolder.getAttributeValue(namespace, name);
    }

}
