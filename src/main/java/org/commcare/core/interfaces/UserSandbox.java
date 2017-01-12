package org.commcare.core.interfaces;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

import java.util.Set;

/**
 *  Interface to be implemented by sandboxes for a user's CommCare instance data
 *
 *  @author wpride1
 */
public abstract class UserSandbox {

    private String syncToken;

    public abstract IStorageUtilityIndexed<Case> getCaseStorage();

    public abstract IStorageUtilityIndexed<Ledger> getLedgerStorage();

    public abstract IStorageUtilityIndexed<User> getUserStorage();

    /**
     * Get user-level (encrypted) storage for a flat fixture
     */
    public abstract IStorageUtilityIndexed<StorageIndexedTreeElementModel> getFlatFixtureStorage(String fixtureName);

    /**
     * Setup flat fixture storage table and indexes over that table.
     * Must clear existing data associated with the given fixture.
     */
    public abstract void setupFlatFixtureStorage(String fixtureName,
                                                 StorageIndexedTreeElementModel exampleEntry,
                                                 Set<String> indices);

    /**
     * Gets the base and child name associated with a fixture id.
     *
     * For example, gets 'products' and 'products' for the data instance
     * "instance('commtrack:products')/products/product/..."
     */
    public abstract Pair<String, String> getFlatFixturePathBases(String fixtureName);

    /**
     * Associates a fixture with a base name and child name.
     *
     * For example, to instantiate a data instance like "instance('commtrack:products')/products/product/..."
     * we must associate 'commtrack:products' with the 'products' base name and the 'product' child name.
     */
    public abstract void setFlatFixturePathBases(String fixtureName, String baseName, String childName);

    public abstract IStorageUtilityIndexed<FormInstance> getUserFixtureStorage();

    public abstract IStorageUtilityIndexed<FormInstance> getAppFixtureStorage();

    public abstract User getLoggedInUser();

    public abstract void setLoggedInUser(User user);

    public void setSyncToken(String syncToken){
        this.syncToken = syncToken;
    }

    public String getSyncToken(){
        return syncToken;
    }
}