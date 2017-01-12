package org.commcare.cases.instance;

import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.InstanceBase;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Hashtable;

/**
 * The root element for the a indexed fixture data instance: instance('some-indexed-fixture').
 * All children are nodes in a database table associated with the fixture.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class IndexedFixtureInstanceTreeElement
        extends StorageInstanceTreeElement<StorageIndexedTreeElementModel, IndexedFixtureChildElement> {
    private Hashtable<XPathPathExpr, String> storageIndexMap = null;

    private IndexedFixtureInstanceTreeElement(AbstractTreeElement instanceRoot,
                                              IStorageUtilityIndexed<StorageIndexedTreeElementModel> storage,
                                              String modelName, String childName) {
        super(instanceRoot, storage, modelName, childName);
    }

    public static IndexedFixtureInstanceTreeElement get(UserSandbox sandbox,
                                                        String instanceName,
                                                        InstanceBase instanceBase) {
        Pair<String, String> modelAndChild =
                sandbox.getIndexedFixturePathBases(instanceName);
        if (modelAndChild == null) {
            return null;
        } else {
            IStorageUtilityIndexed<StorageIndexedTreeElementModel> storage =
                    sandbox.getIndexedFixtureStorage(instanceName);
            return new IndexedFixtureInstanceTreeElement(instanceBase, storage,
                    modelAndChild.first, modelAndChild.second);
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
                String entry = StorageIndexedTreeElementModel.getEntryFromCol(fieldName);
                storageIndexMap.put(XPathReference.getPathExpr(entry), fieldName);
            }
        }

        return storageIndexMap;
    }
}
