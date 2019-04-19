package org.commcare.cases.instance;

import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.commcare.core.interfaces.UserSandbox;
import org.javarosa.core.model.IndexedFixtureIdentifier;
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
    protected byte[] attrHolder;

    public IndexedFixtureInstanceTreeElement(AbstractTreeElement instanceRoot,
                                              IStorageUtilityIndexed<StorageIndexedTreeElementModel> storage,
                                              IndexedFixtureIdentifier indexedFixtureIdentifier) {
        super(instanceRoot, storage, indexedFixtureIdentifier.getFixtureBase(), indexedFixtureIdentifier.getFixtureChild());
        attrHolder = indexedFixtureIdentifier.getRootAttributes();
        cacheKey = indexedFixtureIdentifier.getFixtureBase() + "|" + indexedFixtureIdentifier.getFixtureChild();
    }

    public static IndexedFixtureInstanceTreeElement get(UserSandbox sandbox,
                                                        String instanceName,
                                                        InstanceBase instanceBase) {
        IndexedFixtureIdentifier indexedFixtureIdentifier = sandbox.getIndexedFixtureIdentifier(instanceName);
        if (indexedFixtureIdentifier == null) {
            return null;
        } else {
            IStorageUtilityIndexed<StorageIndexedTreeElementModel> storage =
                    sandbox.getIndexedFixtureStorage(instanceName);
            return new IndexedFixtureInstanceTreeElement(instanceBase, storage, indexedFixtureIdentifier);
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

}
