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
public interface CommCareHomeTransitions  extends SuiteTransitions {
	void viewSuite (Suite suite, Menu m);
	void sendAllUnsent();
	void logout();
	
	void settings ();
	void newUser ();
	void editUsers ();
	void backupRestore ();
	void resetDemo ();
	void upgrade ();
}
