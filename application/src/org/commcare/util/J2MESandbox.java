package org.commcare.util;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.core.properties.CommCareProperties;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.user.model.User;

import main.java.org.javarosa.core.services.storage.IStorageUtilityIndexed;
import main.java.org.javarosa.core.services.storage.StorageManager;

public class J2MESandbox {

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
        return PropertyManager._().getProperty(CommCareProperties.LOGGED_IN_USER);
    }

    public void setLoggedInUser(User user) {

    }

    public void setSyncToken(String syncToken) {

    }

    public String getSyncToken() {
        return null;
    }

    public void updateLastSync() {

    }
}
