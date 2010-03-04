/**
 * 
 */
package org.commcare.applogic;

import java.util.Vector;

import org.commcare.api.transitions.CommCareHomeTransitions;
import org.commcare.util.CommCareBackupRestoreSnapshot;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.commcare.util.CommCareUtil;
import org.commcare.view.CommCareHomeController;
import org.javarosa.cases.util.ICaseType;
import org.javarosa.core.api.State;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.services.properties.api.PropertyUpdateState;

/**
 * @author ctsims
 *
 */
public class CommCareHomeState implements CommCareHomeTransitions, State {

	public void start () {
		CommCareHomeController home = new CommCareHomeController(new Vector<ICaseType>());
		home.setTransitions(this);
		home.start();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.superrosa.api.transitions.SuperRosaHomeTransitions#logout()
	 */
	public void logout() {
		new CommCareLoginState().start();
	}

	public void caseChosen(ICaseType type) {
		//new BracCaseManagementState(type).start();
	}

	public void sendAllUnsent() {
		new SendAllUnsentState () {
			protected SendAllUnsentController getController () {
				return new SendAllUnsentController(new CommCareHQResponder());
			}

			public void done() {
				new CommCareHomeState().start();
			}
		}.start();
	}
	
	public void settings() {
		new PropertyUpdateState () {
			public void done () {
				new CommCareHomeState().start();
			}
		}.start();
	}
	
	public void backupRestore() {
		new CommCareBackupRestoreState(CommCareBackupRestoreSnapshot.class){
			public void done() {
				CommCareUtil.launchHomeState();
			}
		}.start();
	}

	public void newUser() {
		J2MEDisplay.startStateWithLoadingScreen(new CommCareAddUserState());
	}
	
	public void editUsers() {
		throw new RuntimeException("not hooked up yet");
	}
	
	public void reloadForms() {
		throw new RuntimeException("not hooked up yet");
	}

	public void resetDemo() {
		CommCareContext._().resetDemoData();
	}

	public void viewSaved() {
		throw new RuntimeException("not hooked up yet");
	}

}
