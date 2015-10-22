package org.commcare.util;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.properties.CommCareProperties;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.model.User;

import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;

/**
 * Basically just a wrapper around the StorageManager so that we can use that storage acccess
 * as a UserSandbox.
 *
 * @author wspride
 */

public class J2MESandbox extends UserSandbox {

    public IStorageUtilityIndexed<Case> getCaseStorage() {
        return (IStorageUtilityIndexed<Case>)StorageManager.getStorage(Case.STORAGE_KEY);
    }

    public IStorageUtilityIndexed<Ledger> getLedgerStorage() {
        return (IStorageUtilityIndexed<Ledger>)StorageManager.getStorage(Ledger.STORAGE_KEY);
    }

    public IStorageUtilityIndexed<User> getUserStorage() {
        return (IStorageUtilityIndexed<User>)StorageManager.getStorage(User.STORAGE_KEY);
    }

    public IStorageUtilityIndexed<FormInstance> getUserFixtureStorage() {
        return (IStorageUtilityIndexed<FormInstance>)StorageManager.getStorage(FormInstance.STORAGE_KEY);
    }

    public IStorageUtilityIndexed<FormInstance> getAppFixtureStorage() {
        return (IStorageUtilityIndexed<FormInstance>)StorageManager.getStorage(FormInstance.STORAGE_KEY);
    }

    public User getLoggedInUser() {
        IStorageUtilityIndexed<User> users = getUserStorage();
        User user = (User)users.getRecordForValue(User.META_UID, PropertyManager._().getSingularProperty(CommCareProperties.LOGGED_IN_USER));
        return user;
    }

    public void setLoggedInUser(User user) {
        // don't need here
    }
}
