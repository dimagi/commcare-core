package org.commcare.util.mocks;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.cases.model.StorageBackedModel;
import org.commcare.core.interfaces.UserSandbox;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.util.HashMap;

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
    private final HashMap<String, IStorageUtilityIndexed<StorageBackedModel>> flatFixtureStorages;
    private final IStorageUtilityIndexed<FormInstance> userFixtureStorage;
    private IStorageUtilityIndexed<FormInstance> appFixtureStorage;
    
    private User mUser;
    private String mSyncToken;
    private final PrototypeFactory factory;

    /**
     * Create a sandbox of the necessary storage objects with the shared
     * factory.
     *
     * @param factory A prototype factory for deserializing records
     */
    public MockUserDataSandbox(PrototypeFactory factory) {
        caseStorage = new DummyIndexedStorageUtility<>(Case.class, factory);
        ledgerStorage = new DummyIndexedStorageUtility<>(Ledger.class, factory);
        userStorage = new DummyIndexedStorageUtility<>(User.class, factory);
        flatFixtureStorages = new HashMap<>();
        userFixtureStorage = new DummyIndexedStorageUtility<>(FormInstance.class, factory);
        appFixtureStorage = new DummyIndexedStorageUtility<>(FormInstance.class, factory);

        this.factory = factory;
    }

    @Override
    public IStorageUtilityIndexed<Case> getCaseStorage() {
        return caseStorage;
    }

    @Override
    public IStorageUtilityIndexed<Ledger> getLedgerStorage() {
        return ledgerStorage;
    }

    @Override
    public IStorageUtilityIndexed<User> getUserStorage() {
        return userStorage;
    }

    @Override
    public IStorageUtilityIndexed<StorageBackedModel> getFlatFixtureStorage(String fixtureName, Persistable exampleEntry) {
        if (!flatFixtureStorages.containsKey(fixtureName)) {
            // TODO PLM: use exampleEntry instead of StorageBackedModel.class to get meta data correct
            flatFixtureStorages.put(fixtureName, new DummyIndexedStorageUtility<>(StorageBackedModel.class, factory));
        }
        return flatFixtureStorages.get(fixtureName);
    }

    @Override
    public IStorageUtilityIndexed<FormInstance> getUserFixtureStorage() {
        return userFixtureStorage;
    }

    @Override
    public IStorageUtilityIndexed<FormInstance> getAppFixtureStorage() {
        return appFixtureStorage;
    }
    
    @Override
    public void setSyncToken(String syncToken) {
        this.mSyncToken = syncToken;
    }
    
    @Override
    public String getSyncToken() {
        return mSyncToken;
    }
    
    @Override
    public void setLoggedInUser(User user) {
        this.mUser = user;
    }
    
    @Override
    public User getLoggedInUser() {
        return mUser;
    }

    public void setAppFixtureStorageLocation(IStorageUtilityIndexed<FormInstance>
                                                     appFixtureStorageLocation) {
        this.appFixtureStorage = appFixtureStorageLocation;
    }
}
