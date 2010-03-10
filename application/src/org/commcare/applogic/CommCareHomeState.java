/**
 * 
 */
package org.commcare.applogic;

import java.util.Vector;

import org.commcare.api.transitions.CommCareHomeTransitions;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareBackupRestoreSnapshot;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.commcare.util.CommCareManager;
import org.commcare.util.CommCareUtil;
import org.commcare.view.CommCareHomeController;
import org.javarosa.core.api.State;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.services.properties.api.PropertyUpdateState;

/**
 * @author ctsims
 *
 */
public class CommCareHomeState implements CommCareHomeTransitions, State {

	public void start () {
		CommCareHomeController home = new CommCareHomeController(CommCareManager._().getInstalledSuites());
		home.setTransitions(this);
		home.start();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.superrosa.api.transitions.SuperRosaHomeTransitions#logout()
	 */
	public void logout() {
		new CommCareLoginState().start();
	}

	public void viewSuite(Suite suite, Menu m) {
		CommCareSuiteHomeState state = new CommCareSuiteHomeState(suite, m) {

			public void exit() {
				CommCareUtil.launchHomeState();
			}
			
		};
		J2MEDisplay.startStateWithLoadingScreen(state);
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

	public void entry(Entry entry) {
		CommCareUtil.launchEntry(entry,this);
	}

	public void exit() {
		logout();
	}

	public void upgrade() {
		CommCareManager._().upgrade();
	}
}
