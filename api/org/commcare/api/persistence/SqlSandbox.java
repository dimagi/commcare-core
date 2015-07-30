package org.commcare.api.persistence;

import org.commcare.api.interfaces.UserDataInterface;
import org.commcare.api.models.SqlMeta;
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
public class SqlSandbox implements UserDataInterface{
    private final SqlIndexedStorageUtility<Case> caseStorage;
    private final SqlIndexedStorageUtility<Ledger> ledgerStorage;
    private final SqlIndexedStorageUtility<User> userStorage;
    private final SqlIndexedStorageUtility<FormInstance> userFixtureStorage;
    private final SqlIndexedStorageUtility<FormInstance> appFixtureStorage;
    private final SqlIndexedStorageUtility<SqlMeta> metaDataStorage;
    private User user;

    /**
     * Create a sandbox of the necessary storage objects with the shared
     * factory.
     *
     * @param factory A prototype factory for deserializing records
     */
    public SqlSandbox(PrototypeFactory factory, String username, boolean clear) {
        caseStorage = new SqlIndexedStorageUtility<Case>(Case.class, factory, username, "TFCase");
        ledgerStorage = new SqlIndexedStorageUtility<Ledger>(Ledger.class, factory, username, "Ledger");
        userStorage = new SqlIndexedStorageUtility<User>(User.class, factory, username, "User");
        userFixtureStorage = new SqlIndexedStorageUtility<FormInstance>(FormInstance.class, factory, username, "UserFixture");
        appFixtureStorage = new SqlIndexedStorageUtility<FormInstance>(FormInstance.class, factory, username, "AppFixture");
        metaDataStorage = new SqlIndexedStorageUtility<SqlMeta>(SqlMeta.class, factory, username, "Meta");

        if(clear){
            caseStorage.resetTable();
            ledgerStorage.resetTable();
            userStorage.resetTable();
            userFixtureStorage.resetTable();
            appFixtureStorage.resetTable();
            metaDataStorage.resetTable();
        }
    }

    /**
     * Create a sandbox of the necessary storage objects with the shared
     * factory.
     *
     * @param factory A prototype factory for deserializing records
     */
    public SqlSandbox(PrototypeFactory factory, String username) {
        this(factory, username, false);
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

    public SqlIndexedStorageUtility<SqlMeta> getMetaStorage() {
        return metaDataStorage;
    }

    public void updateLastSync(){
        SqlIndexedStorageUtility<SqlMeta> mStorage = getMetaStorage();
        mStorage.resetTable();
        SqlMeta mSqlMeta = new SqlMeta();
        mStorage.write(mSqlMeta);
    }

    public Date getLastSync(){
        SqlIndexedStorageUtility<SqlMeta> mStorage = getMetaStorage();
        if(mStorage == null){
            return null;
        }
        SqlMeta mSqlMeta = mStorage.read(1);
        if(mSqlMeta == null){
            return null;
        }
        Date mDate = (Date)mSqlMeta.getMetaData(SqlMeta.META_LAST_SYNC);
        return mDate;
    }

}
