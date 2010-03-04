/**
 * 
 */
package org.commcare.api.transitions;

import org.javarosa.cases.util.ICaseType;

/**
 * @author ctsims
 *
 */
public interface CommCareHomeTransitions {
	void caseChosen (ICaseType type);
	void sendAllUnsent();
	void logout();
	
	void settings ();
	void newUser ();
	void editUsers ();
	void backupRestore ();
	void resetDemo ();
}
