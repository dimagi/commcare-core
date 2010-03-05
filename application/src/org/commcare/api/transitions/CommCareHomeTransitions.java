/**
 * 
 */
package org.commcare.api.transitions;

import org.commcare.suite.model.Suite;

/**
 * @author ctsims
 *
 */
public interface CommCareHomeTransitions {
	void viewSuite (Suite suite);
	void sendAllUnsent();
	void logout();
	
	void settings ();
	void newUser ();
	void editUsers ();
	void backupRestore ();
	void resetDemo ();
}
