package org.commcare.util.mocks;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.core.interfaces.UserSandbox;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.util.Date;

/**
 * A placeholder for the in-memory storage elements needed for an individual
 * CommCare user.
 *
 * Uses a shared factory to appropriately manage prototype hashing, so can be
 * used as a reliable source of reads/writes for in-memory mocks of stoage
 * objects
 *
 * @author ctsims
 */
public class MockUserDataSandbox extends UserSandbox {

    private final IStorageUtilityIndexed<Case> caseStorage;
    private final IStorageUtilityIndexed<Ledger> ledgerStorage;
    private final IStorageUtilityIndexed<User> userStorage;
    private final IStorageUtilityIndexed<FormInstance> userFixtureStorage;
    private IStorageUtilityIndexed<FormInstance> appFixtureStorage;
    
    private User mUser;
    
    private String mSyncToken;

    private Date lastSync;

    /**
     * Create a sandbox of the necessary storage objects with the shared
     * factory.
     *
     * @param factory A prototype factory for deserializing records
     */
    public MockUserDataSandbox(PrototypeFactory factory) {
        caseStorage = new DummyIndexedStorageUtility<Case>(Case.class, factory);
        ledgerStorage = new DummyIndexedStorageUtility<Ledger>(Ledger.class, factory);
        userStorage = new DummyIndexedStorageUtility<User>(User.class, factory);
        userFixtureStorage = new DummyIndexedStorageUtility<FormInstance>(FormInstance.class, factory);
        appFixtureStorage = new DummyIndexedStorageUtility<FormInstance>(FormInstance.class, factory);
    }

    public IStorageUtilityIndexed<Case> getCaseStorage() {
        return caseStorage;
    }

    public IStorageUtilityIndexed<Ledger> getLedgerStorage() {
        return ledgerStorage;
    }

    public IStorageUtilityIndexed<User> getUserStorage() {
        return userStorage;
    }

    public IStorageUtilityIndexed<FormInstance> getUserFixtureStorage() {
        return userFixtureStorage;
    }

    public IStorageUtilityIndexed<FormInstance> getAppFixtureStorage() {
        return appFixtureStorage;
    }
    
    public void setSyncToken(String syncToken) {
        this.mSyncToken = syncToken;
    }
    
    public String getSyncToken() {
        return mSyncToken;
    }
    
    public void setLoggedInUser(User user) {
        this.mUser = user;
    }
    
    public User getLoggedInUser() {
        return mUser;
    }

    public void setAppFixtureStorageLocation(IStorageUtilityIndexed<FormInstance>
                                                     appFixtureStorageLocation) {
        this.appFixtureStorage = appFixtureStorageLocation;
    }
}
