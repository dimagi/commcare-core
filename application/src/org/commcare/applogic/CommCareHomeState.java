/**
 * 
 */
package org.commcare.applogic;

import org.commcare.api.transitions.CommCareHomeTransitions;
import org.commcare.entity.RecentFormEntity;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.commcare.util.CommCareUtil;
import org.commcare.view.CommCareHomeController;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.cases.model.Case;
import org.javarosa.chsreferral.model.PatientReferral;
import org.javarosa.core.api.State;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.formmanager.api.FormEntryState;
import org.javarosa.formmanager.api.JrFormEntryController;
import org.javarosa.formmanager.api.JrFormEntryModel;
import org.javarosa.formmanager.utility.FormDefFetcher;
import org.javarosa.formmanager.utility.ModelRmsRetrievalMethod;
import org.javarosa.formmanager.view.chatterbox.Chatterbox;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.services.properties.api.PropertyUpdateState;
import org.javarosa.user.model.User;
import org.javarosa.user.utility.UserEntity;

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
		CommCareUtil.exitMain();
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
		J2MEDisplay.startStateWithLoadingScreen(new SendAllUnsentState () {
			protected SendAllUnsentController getController () {
				return new SendAllUnsentController(new CommCareHQResponder());
			}

			public void done() {
				new CommCareHomeState().start();
			}
		});
	}
	
	public void settings() {
		J2MEDisplay.startStateWithLoadingScreen(new PropertyUpdateState () {
			public void done () {
				new CommCareHomeState().start();
			}
		});
	}
	
	public void restoreUserData() {
		J2MEDisplay.startStateWithLoadingScreen(new CommCareOTARestoreState() {

			public void cancel() {
				new CommCareHomeState().start();
			}

			public void done() {
				new CommCareHomeState().start();
			}
			
		});
	}
	
	private void clearUserData() {
		StorageManager.getStorage(User.STORAGE_KEY).removeAll(new EntityFilter<User>() {

			public boolean matches(User e) {
				if(e.isAdminUser()) { return false;} 
				return true;
			}
			
		});
		StorageManager.getStorage(Case.STORAGE_KEY).removeAll();
		StorageManager.getStorage(PatientReferral.STORAGE_KEY).removeAll();
	}

	public void newUser() {
		J2MEDisplay.startStateWithLoadingScreen(new CommCareAddUserState());
	}
	
	public void editUsers() {
		J2MEDisplay.startStateWithLoadingScreen(new CommCareSelectState<User>(new UserEntity(), User.STORAGE_KEY) {

			public void cancel() {
				CommCareUtil.launchHomeState();
			}

			public void entitySelected(int id) {
				User u = (User)StorageManager.getStorage(User.STORAGE_KEY).read(id);
				J2MEDisplay.startStateWithLoadingScreen(new CommCareEditUserState(u));
			}
		});
	}
	
	public void reloadForms() {
		throw new RuntimeException("not hooked up yet");
	}

	public void resetDemo() {
		//CommCareContext._().autoPurge();
		CommCareContext._().resetDemoData();
	}
	
	public void review() {
		final RecentFormEntity prototype = new RecentFormEntity(CommCareContext._().getManager().getInstalledSuites());
		J2MEDisplay.startStateWithLoadingScreen(new CommCareSelectState<FormInstance>(prototype, FormInstance.STORAGE_KEY) {
			
			public void entitySelected(final int instanceID) {
				//Man this is dumb....
				FormInstance instance = (FormInstance)StorageManager.getStorage(FormInstance.STORAGE_KEY).read(instanceID);
				final String title = prototype.getTypeName(instance.schema);
				J2MEDisplay.startStateWithLoadingScreen(new FormEntryState () {
					protected JrFormEntryController getController() {
						FormDefFetcher fetcher = new FormDefFetcher(new ModelRmsRetrievalMethod(instanceID), instanceID, CommCareContext._().getPreloaders(), CommCareContext._().getFuncHandlers());
						JrFormEntryController controller = CommCareUtil.createFormEntryController(new JrFormEntryModel(fetcher.getFormDef(), true));
						controller.setView(new Chatterbox(title, controller));
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
		try {
			CommCareContext._().getManager().upgrade(CommCareContext.RetrieveGlobalResourceTable(), CommCareContext.CreateTemporaryResourceTable("UPGRADGE"));
		} catch (UnfullfilledRequirementsException e) {
			J2MEDisplay.showError(null,Localization.get("commcare.noupgrade.version"));
		}
	}
}
