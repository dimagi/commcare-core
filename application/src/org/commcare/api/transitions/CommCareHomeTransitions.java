/**
 *
 */
package org.commcare.api.transitions;

import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;

/**
 * @author ctsims
 *
 */
public interface CommCareHomeTransitions  extends MenuTransitions {
    void sessionItemChosen(int item);
    void sendAllUnsent();
    void serverSync();
    void logout();

    void settings ();
    void newUser ();
    void editUsers ();
    void restoreUserData ();
    void resetDemo ();
    void upgrade ();
    void review ();
    void adminLogin ();
    void forceSend ();

    // dev / debugging
    void rmsdump ();
    void viewLogs ();
    void gprsTest ();
    void permissionsTest();
}
