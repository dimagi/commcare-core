package org.commcare.cases.instance;

import org.commcare.cases.model.StorageBackedModel;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Hashtable;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FlatFixtureInstanceTreeElement extends StorageInstanceTreeElement<StorageBackedModel, FixtureChildElement> {

    public FlatFixtureInstanceTreeElement(AbstractTreeElement instanceRoot,
                                          IStorageUtilityIndexed<StorageBackedModel> storage,
                                          String modelName, String childName) {
        super(instanceRoot, storage, modelName, childName);
    }

    @Override
    protected FixtureChildElement buildElement(StorageInstanceTreeElement<StorageBackedModel, FixtureChildElement> storageInstance,
                                               int recordId, String id, int mult) {
        return new FixtureChildElement(storageInstance, mult, recordId, null);
    }

    @Override
    protected FixtureChildElement getChildTemplate() {
        return FixtureChildElement.buildFixtureChildTemplate(this);
    }

    @Override
    protected Hashtable<XPathPathExpr, String> getStorageIndexMap() {
        return null;
    }
}
