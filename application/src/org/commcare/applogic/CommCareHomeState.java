/**
 * 
 */
package org.commcare.applogic;

import org.commcare.api.transitions.CommCareHomeTransitions;
import org.commcare.entity.RecentFormEntity;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareBackupRestoreSnapshot;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.commcare.util.CommCareUtil;
import org.commcare.view.CommCareHomeController;
import org.javarosa.core.api.State;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.formmanager.api.FormEntryState;
import org.javarosa.formmanager.api.JrFormEntryController;
import org.javarosa.formmanager.api.JrFormEntryModel;
import org.javarosa.formmanager.utility.FormDefFetcher;
import org.javarosa.formmanager.utility.ModelRmsRetrievalMethod;
import org.javarosa.formmanager.view.chatterbox.Chatterbox;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.services.properties.api.PropertyUpdateState;

/**
 * @author ctsims
 *
 */
public class CommCareHomeState implements CommCareHomeTransitions, State {

	public void start () {
		CommCareHomeController home = new CommCareHomeController(CommCareContext._().getManager().getInstalledSuites(), CommCareContext._().getManager().getCurrentProfile());
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
	
	public void review() {
		J2MEDisplay.startStateWithLoadingScreen(new CommCareSelectState<FormInstance>(new RecentFormEntity(CommCareContext._().getManager().getInstalledSuites()), FormInstance.STORAGE_KEY) {
			
			public void entitySelected(final int instanceID) {
				J2MEDisplay.startStateWithLoadingScreen(new FormEntryState () {
					protected JrFormEntryController getController() {
						FormDefFetcher fetcher = new FormDefFetcher(new ModelRmsRetrievalMethod(instanceID), instanceID,
								CommCareContext._().getPreloaders(), CommCareContext._().getFuncHandlers());
						JrFormEntryController controller = new JrFormEntryController(new JrFormEntryModel(fetcher.getFormDef(), true));
						controller.setView(new Chatterbox("Chatterbox", controller));
						return controller;
					}
						
					public void abort() {
						CommCareUtil.launchHomeState();
					}
	
					public void formEntrySaved(FormDef form, FormInstance instanceData, boolean formWasCompleted) {
						CommCareUtil.launchHomeState();
					}
	
					public void suspendForMediaCapture(int captureType) {
						throw new RuntimeException("not applicable");
					}
				});
			}

			public void cancel() {
				CommCareUtil.launchHomeState();
			}
		});
	}

	public void viewSaved() {
		throw new RuntimeException("not hooked up yet");
	}

	public void entry(Suite suite, Entry entry) {
		CommCareUtil.launchEntry(suite, entry,this);
	}

	public void exit() {
		logout();
	}

	public void upgrade() {
		CommCareContext._().getManager().upgrade();
	}
}
