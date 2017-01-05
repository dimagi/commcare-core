package org.commcare.cases.instance;

import org.commcare.cases.model.StorageBackedModel;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.util.Pair;
import org.commcare.xml.FlatFixtureXmlParser;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.InstanceBase;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Hashtable;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FlatFixtureInstanceTreeElement extends StorageInstanceTreeElement<StorageBackedModel, FixtureChildElement> {
    private Hashtable<XPathPathExpr, String> storageIndexMap = null;

    private FlatFixtureInstanceTreeElement(AbstractTreeElement instanceRoot,
                                          IStorageUtilityIndexed<StorageBackedModel> storage,
                                          String modelName, String childName) {
        super(instanceRoot, storage, modelName, childName);
    }

    public static FlatFixtureInstanceTreeElement get(UserSandbox sandbox,
                                                     InstanceBase instanceBase) {
        Pair<String, String> modelAndChild = sandbox.getFlatFixturePathBases(instanceBase.getInstanceName());
        IStorageUtilityIndexed<StorageBackedModel> storage =
                sandbox.getFlatFixtureStorage(instanceBase.getInstanceName(), null);
        return new FlatFixtureInstanceTreeElement(instanceBase, storage, modelAndChild.first, modelAndChild.second);
    }

    @Override
    protected FixtureChildElement buildElement(StorageInstanceTreeElement<StorageBackedModel, FixtureChildElement> storageInstance,
                                               int recordId, String id, int mult) {
        return new FixtureChildElement(storageInstance, mult, recordId);
    }

    @Override
    protected FixtureChildElement getChildTemplate() {
        return FixtureChildElement.buildFixtureChildTemplate(this);
    }

    @Override
    protected Hashtable<XPathPathExpr, String> getStorageIndexMap() {
        if (storageIndexMap == null) {
            storageIndexMap = new Hashtable<>();

            StorageBackedModel template = getModelTemplate();
            for (String elementName : template.getElements().keySet()) {
                storageIndexMap.put(XPathReference.getPathExpr(elementName), elementName);
            }
            for (String attrName : template.getAttributes().keySet()) {
                String uniqueAttrColName =
                        FlatFixtureXmlParser.getAttributeColumnName(attrName, template.getElements().keySet());
                storageIndexMap.put(XPathReference.getPathExpr(attrName), uniqueAttrColName);
            }
        }

        return storageIndexMap;
    }
}
