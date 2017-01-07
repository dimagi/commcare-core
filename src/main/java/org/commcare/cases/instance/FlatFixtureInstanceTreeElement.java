package org.commcare.cases.instance;

import org.commcare.cases.model.StorageBackedModel;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.InstanceBase;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Hashtable;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FlatFixtureInstanceTreeElement
        extends StorageInstanceTreeElement<StorageBackedModel, FlatFixtureChildElement> {
    private Hashtable<XPathPathExpr, String> storageIndexMap = null;

    private FlatFixtureInstanceTreeElement(AbstractTreeElement instanceRoot,
                                           IStorageUtilityIndexed<StorageBackedModel> storage,
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
            IStorageUtilityIndexed<StorageBackedModel> storage =
                    sandbox.getFlatFixtureStorage(instanceName, null);
            return new FlatFixtureInstanceTreeElement(instanceBase, storage,
                    modelAndChild.first, modelAndChild.second);
        }
    }

    @Override
    protected FlatFixtureChildElement buildElement(StorageInstanceTreeElement<StorageBackedModel, FlatFixtureChildElement> storageInstance,
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

            StorageBackedModel template = getModelTemplate();
            for (String attrName : template.getAttributes().keySet()) {
                storageIndexMap.put(XPathReference.getPathExpr(attrName),
                        StorageBackedModel.getColumnName(attrName));
            }
            for (String elementName : template.getElements().keySet()) {
                String escapedElem =
                        StorageBackedModel.getUniqueColumnName(elementName, template.getEscapedAttributeKeys());
                storageIndexMap.put(XPathReference.getPathExpr(elementName), escapedElem);
            }
        }

        return storageIndexMap;
    }
}
