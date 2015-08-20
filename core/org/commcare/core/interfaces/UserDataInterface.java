package org.commcare.core.interfaces;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.suite.model.User;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

/**
 *  Interface to be implemented by sandboxes for a user's CommCare instance data
 *  @author wpride1
 */

public interface UserDataInterface{

    IStorageUtilityIndexed<Case> getCaseStorage();

    IStorageUtilityIndexed<Ledger> getLedgerStorage();

    IStorageUtilityIndexed<User> getUserStorage();

    IStorageUtilityIndexed<FormInstance> getUserFixtureStorage();

    IStorageUtilityIndexed<FormInstance> getAppFixtureStorage();

    User getLoggedInUser();

    void setLoggedInUser(User user);

    void setSyncToken(String syncToken);

    String getSyncToken();

}