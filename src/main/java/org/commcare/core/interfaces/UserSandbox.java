package org.commcare.core.interfaces;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.cases.model.StorageBackedModel;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;

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

    public abstract IStorageUtilityIndexed<StorageBackedModel> getFlatFixtureStorage(String fixtureName, Persistable exampleEntry);

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