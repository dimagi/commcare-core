/**
 *
 */
package org.commcare.api.transitions;

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
