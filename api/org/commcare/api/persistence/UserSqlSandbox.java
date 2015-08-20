package org.commcare.api.persistence;

import org.commcare.core.interfaces.UserDataInterface;
import org.commcare.core.models.UserSandboxMetaData;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.suite.model.User;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.util.Date;

/**
 * A sandbox using SqlIndexedStorageUtility
 *
 * @author wspride
 */
public class UserSqlSandbox implements UserDataInterface{
    private final SqlIndexedStorageUtility<Case> caseStorage;
    private final SqlIndexedStorageUtility<Ledger> ledgerStorage;
    private final SqlIndexedStorageUtility<User> userStorage;
    private final SqlIndexedStorageUtility<FormInstance> userFixtureStorage;
    private final SqlIndexedStorageUtility<FormInstance> appFixtureStorage;
    private final SqlIndexedStorageUtility<UserSandboxMetaData> metaDataStorage;
    private User user;
    String username;

    /**
     * Create a sandbox of the necessary storage objects with the shared
     * factory.
     *
     * @param factory A prototype factory for deserializing records
     */
    public UserSqlSandbox(PrototypeFactory factory, String username) {
        this.username = username;
        caseStorage = new SqlIndexedStorageUtility<Case>(Case.class, factory, username, "TFCase");
        ledgerStorage = new SqlIndexedStorageUtility<Ledger>(Ledger.class, factory, username, "Ledger");
        userStorage = new SqlIndexedStorageUtility<User>(User.class, factory, username, "User");
        userFixtureStorage = new SqlIndexedStorageUtility<FormInstance>(FormInstance.class, factory, username, "UserFixture");
        appFixtureStorage = new SqlIndexedStorageUtility<FormInstance>(FormInstance.class, factory, username, "AppFixture");
        metaDataStorage = new SqlIndexedStorageUtility<UserSandboxMetaData>(UserSandboxMetaData.class, factory, username, "Meta");
    }


    public SqlIndexedStorageUtility<Case> getCaseStorage() {
        return caseStorage;
    }

    public SqlIndexedStorageUtility<Ledger> getLedgerStorage() {
        return ledgerStorage;
    }

    public SqlIndexedStorageUtility<User> getUserStorage() {
        return userStorage;
    }

    public SqlIndexedStorageUtility<FormInstance> getUserFixtureStorage() {
        return userFixtureStorage;
    }

    public SqlIndexedStorageUtility<FormInstance> getAppFixtureStorage() {
        return appFixtureStorage;
    }

    @Override
    public User getLoggedInUser() {
        SqlIndexedStorageUtility<User> userStorage = getUserStorage();
        SqlStorageIterator<User> iterator = userStorage.iterate();
        if(iterator.hasMore()){
            return iterator.next();
        }
        return user;
    }

    @Override
    public void setLoggedInUser(User user) {
        this.user = user;
    }

    @Override
    public void setSyncToken(String syncToken) {
        //TODO
    }

    @Override
    public String getSyncToken() {
        //TODO
        return "TODO";
    }

    public SqlIndexedStorageUtility<UserSandboxMetaData> getMetaStorage() {
        return metaDataStorage;
    }

    public void updateLastSync(){
        SqlIndexedStorageUtility<UserSandboxMetaData> mStorage = getMetaStorage();
        UserSandboxMetaData mUserSandboxMetaData = new UserSandboxMetaData();
        mStorage.write(mUserSandboxMetaData);
    }

    public Date getLastSync(){
        SqlIndexedStorageUtility<UserSandboxMetaData> mStorage = getMetaStorage();
        if(mStorage == null){
            return null;
        }
        UserSandboxMetaData mUserSandboxMetaData = mStorage.read(1);
        if(mUserSandboxMetaData == null){
            return null;
        }
        Date mDate = (Date) mUserSandboxMetaData.getMetaData(UserSandboxMetaData.META_LAST_SYNC);
        return mDate;
    }

}
