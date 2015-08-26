package org.commcare.api.persistence;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.core.interfaces.UserDataInterface;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * A sandbox for user data using SqlIndexedStorageUtility. Sandbox is per-User
 *
 * @author wspride
 */
public class UserSqlSandbox implements UserDataInterface{
    private final SqlIndexedStorageUtility<Case> caseStorage;
    private final SqlIndexedStorageUtility<Ledger> ledgerStorage;
    private final SqlIndexedStorageUtility<User> userStorage;
    private final SqlIndexedStorageUtility<FormInstance> userFixtureStorage;
    private final SqlIndexedStorageUtility<FormInstance> appFixtureStorage;
    private User user = null;

    /**
     * Create a sandbox of the necessary storage objects with the shared
     * factory.
     *
     * @param factory A prototype factory for deserializing records
     */
    public UserSqlSandbox(PrototypeFactory factory, String username) {
        //we can't name this table "Case" becase that's reserved by sqlite
        caseStorage = new SqlIndexedStorageUtility<Case>(Case.class, factory, username, "CCCase");
        ledgerStorage = new SqlIndexedStorageUtility<Ledger>(Ledger.class, factory, username, "Ledger");
        userStorage = new SqlIndexedStorageUtility<User>(User.class, factory, username, "User");
        userFixtureStorage = new SqlIndexedStorageUtility<FormInstance>(FormInstance.class, factory, username, "UserFixture");
        appFixtureStorage = new SqlIndexedStorageUtility<FormInstance>(FormInstance.class, factory, username, "AppFixture");
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
        if(user == null){
            SqlIndexedStorageUtility<User> userStorage = getUserStorage();
            SqlStorageIterator<User> iterator = userStorage.iterate();
            if(iterator.hasMore()){
                // should be only one user here
                user =  iterator.next();
            } else {
                return null;
            }
        }
        return user;
    }

    @Override
    public void setLoggedInUser(User user) {
        this.user = user;
    }

    // implement sync token stuff in next iteration, but useful to have in superclass now for AndroidSandbox

    @Override
    public void setSyncToken(String syncToken) {
        //TODO
    }

    @Override
    public String getSyncToken() {
        //TODO
        return "TODO";
    }
}
