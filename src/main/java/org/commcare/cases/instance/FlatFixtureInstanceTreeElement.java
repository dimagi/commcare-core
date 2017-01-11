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
 * The root element for the a flat fixture data instance: instance('some-flat-fixture').
 * All children are nodes in a database table associated with the fixture.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FlatFixtureInstanceTreeElement
        extends StorageInstanceTreeElement<StorageIndexedTreeElementModel, FlatFixtureChildElement> {
    private Hashtable<XPathPathExpr, String> storageIndexMap = null;

    private FlatFixtureInstanceTreeElement(AbstractTreeElement instanceRoot,
                                           IStorageUtilityIndexed<StorageIndexedTreeElementModel> storage,
                                           String modelName, String childName) {
        super(instanceRoot, storage, modelName, childName);
    }

    public static FlatFixtureInstanceTreeElement get(UserSandbox sandbox,
                                                     String instanceName,
                                                     InstanceBase instanceBase) {
        Pair<String, String> modelAndChild =
                sandbox.getFlatFixturePathBases(instanceName);
        if (modelAndChild == null) {
            return null;
        } else {
            IStorageUtilityIndexed<StorageIndexedTreeElementModel> storage =
                    sandbox.getFlatFixtureStorage(instanceName, null);
            return new FlatFixtureInstanceTreeElement(instanceBase, storage,
                    modelAndChild.first, modelAndChild.second);
        }
    }

    @Override
    protected FlatFixtureChildElement buildElement(StorageInstanceTreeElement<StorageIndexedTreeElementModel, FlatFixtureChildElement> storageInstance,
                                                   int recordId, String id, int mult) {
        return new FlatFixtureChildElement(storageInstance, mult, recordId);
    }

    @Override
    protected FlatFixtureChildElement getChildTemplate() {
        return FlatFixtureChildElement.buildFixtureChildTemplate(this);
    }

    @Override
    protected Hashtable<XPathPathExpr, String> getStorageIndexMap() {
        if (storageIndexMap == null) {
            storageIndexMap = new Hashtable<>();

            StorageIndexedTreeElementModel template = getModelTemplate();
            for (String fieldName : template.getMetaDataFields()) {
                String entry;
                if (StorageIndexedTreeElementModel.isAttrCol(fieldName)) {
                    entry = StorageIndexedTreeElementModel.getAttrFromCol(fieldName);
                } else if (StorageIndexedTreeElementModel.isElemCol(fieldName)) {
                    entry = StorageIndexedTreeElementModel.getElemFromCol(fieldName);
                } else {
                    throw new RuntimeException("Unable to process index of '" + fieldName +"' metadat entry");
                }
                storageIndexMap.put(XPathReference.getPathExpr(entry), fieldName);
            }
        }

        return storageIndexMap;
    }
}
