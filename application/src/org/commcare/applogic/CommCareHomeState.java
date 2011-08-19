/**
 * 
 */
package org.commcare.applogic;

import org.commcare.api.transitions.CommCareHomeTransitions;
import org.commcare.core.properties.CommCareProperties;
import org.commcare.entity.RecentFormEntity;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.commcare.util.CommCareInitializer;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
import org.commcare.util.CommCareSessionController;
import org.commcare.util.CommCareUtil;
import org.commcare.util.InitializationListener;
import org.commcare.util.YesNoListener;
import org.commcare.view.CommCareHomeController;
import org.commcare.view.CommCareStartupInteraction;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.cases.model.Case;
import org.javarosa.chsreferral.model.PatientReferral;
import org.javarosa.core.api.State;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.formmanager.api.FormEntryState;
import org.javarosa.formmanager.api.JrFormEntryController;
import org.javarosa.formmanager.api.JrFormEntryModel;
import org.javarosa.formmanager.utility.FormDefFetcher;
import org.javarosa.formmanager.utility.ModelRmsRetrievalMethod;
import org.javarosa.formmanager.view.chatterbox.Chatterbox;
import org.javarosa.j2me.log.viewer.LogViewerState;
import org.javarosa.j2me.util.DumpRMS;
import org.javarosa.j2me.util.GPRSTestState;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.services.properties.api.PropertyUpdateState;
import org.javarosa.user.model.User;
import org.javarosa.user.utility.UserEntity;

/**
 * @author ctsims
 *
 */
public class CommCareHomeState implements CommCareHomeTransitions, State {

	CommCareSessionController sessionController;
	
	public void start () {
		sessionController = new CommCareSessionController(new CommCareSession(CommCareContext._().getManager()), this);
		CommCareHomeController home = new CommCareHomeController(CommCareContext._().getManager().getInstalledSuites(), CommCareContext._().getManager().getCurrentProfile(), sessionController);
		home.setTransitions(this);
		home.start();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.superrosa.api.transitions.SuperRosaHomeTransitions#logout()
	 */
	public void logout() {
		CommCareUtil.exitMain();
	}
	

	public void sessionItemChosen(int item) {
		//this hands off control to the session until it returns here.
		sessionController.chooseSessionItem(item);
		sessionController.next();
	}

	public void sendAllUnsent() {
		J2MEDisplay.startStateWithLoadingScreen(new SendAllUnsentState () {
			protected SendAllUnsentController getController () {
				return new SendAllUnsentController(new CommCareHQResponder(PropertyManager._().getSingularProperty(JavaRosaPropertyRules.OPENROSA_API_LEVEL)));
			}

			public void done() {
				new CommCareHomeState().start();
			}
		});
	}
	

	
	public void serverSync () {
		J2MEDisplay.startStateWithLoadingScreen(new ServerSyncState (CommCareContext._().getCurrentUserCredentials()) {
			public void onSuccess (String detail) {
				J2MEDisplay.startStateWithLoadingScreen(CommCareUtil.alertFactory("Update", detail));
			}
			
			public void onError (String detail) {
				J2MEDisplay.startStateWithLoadingScreen(CommCareUtil.alertFactory("Failed to update", detail));
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

			public void done(boolean errorsOccured) {
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
		J2MEDisplay.startStateWithLoadingScreen(new CommCareAddUserState(
				!CommCareProperties.USER_REG_SKIP.equals(PropertyManager._().getSingularProperty(CommCareProperties.USER_REG_TYPE)),
				PropertyManager._().getSingularProperty(JavaRosaPropertyRules.OPENROSA_API_LEVEL)));
	}
	
	public void editUsers() {
		J2MEDisplay.startStateWithLoadingScreen(new CommCareSelectState<User>(new UserEntity(), User.STORAGE_KEY) {

			public void cancel() {
				CommCareUtil.launchHomeState();
			}

			public void entitySelected(int id) {
				User u = (User)StorageManager.getStorage(User.STORAGE_KEY).read(id);
				J2MEDisplay.startStateWithLoadingScreen(new CommCareEditUserState(u,	
						!CommCareProperties.USER_REG_SKIP.equals(PropertyManager._().getSingularProperty(CommCareProperties.USER_REG_TYPE)),
						PropertyManager._().getSingularProperty(JavaRosaPropertyRules.OPENROSA_API_LEVEL)));
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
		//Not relevant anymore
		//CommCareUtil.launchEntry(suite, entry,this);
	}

	public void exitMenuTransition() {
		logout();
	}

	public void upgrade() {
		final CommCareStartupInteraction interaction  = new CommCareStartupInteraction("Checking for updates....");
		J2MEDisplay.setView(interaction);
		
		CommCareInitializer upgradeInitializer = new CommCareInitializer() {

			protected boolean runWrapper() throws UnfullfilledRequirementsException {
				
				ResourceTable upgrade = CommCareContext.CreateTemporaryResourceTable("UPGRADGE");
				ResourceTable global = CommCareContext.RetrieveGlobalResourceTable();
				
				boolean staged = false;
				
				while(!staged) {
					try {
						CommCareContext._().getManager().stageUpgradeTable(CommCareContext.RetrieveGlobalResourceTable(), upgrade);
						staged = true;
					} catch (StorageFullException e) {
						Logger.die("Upgrade", e);
					} catch (UnresolvedResourceException e) {
						if(blockForResponse("Couldn't find the update profile, do you want to try again?")) {
							//loop here
						} else {
							return false;
						}
					}
				}
				
				Resource updateProfile = upgrade.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);
				
				Resource currentProfile = global.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);
				
				if(!(updateProfile.getVersion() > currentProfile.getVersion())){
					blockForResponse("CommCare is up to date!", false);
					return true;
				}
				if(!blockForResponse("Upgrade is Available! Do you want to start the update?")) {
					return true;
				}
				
				setMessage("Updating Installation...");
				CommCareContext._().getManager().upgrade(global, upgrade);
				
				blockForResponse("CommCare Updated!", false);
				return true;
			}

			protected void setMessage(String message) {
				interaction.setMessage(message,true);
			}

			protected void askForResponse(String message, YesNoListener listener, boolean yesNo) {
				interaction.setMessage(message,false);
				if(yesNo) {
					interaction.AskYesNo(message, listener);
				} else { 
					interaction.PromptResponse(message, listener);
				}
			}
			
			protected void fail(Exception e) {
				Logger.exception(e);
				blockForResponse("An error occured during the upgrade!", false);
				CommCareUtil.launchHomeState();
			}
		};
		
		upgradeInitializer.initialize(new InitializationListener() {

			public void onSuccess() {
				CommCareUtil.launchHomeState();
			}

			public void onFailure() {
				CommCareUtil.launchHomeState();
			}
			
		});
		//J2MEDisplay.showError(null,Localization.get("commcare.noupgrade.version"));
	}
	
	public void rmsdump () {
		try {
			DumpRMS.dumpRMS(CommCareContext._().getMidlet().getAppProperty("RMS-Image-Path"));
			J2MEDisplay.startStateWithLoadingScreen(CommCareUtil.alertFactory("RMS Dump", "Dump successful!"));
		} catch (Exception e) {
			Logger.exception(e);
			J2MEDisplay.startStateWithLoadingScreen(CommCareUtil.alertFactory("RMS Dump Failed!", WrappedException.printException(e)));
		}
	}
	
	public void viewLogs () {
		J2MEDisplay.startStateWithLoadingScreen(new LogViewerState () {
			public void done() {
				new CommCareHomeState().start();
			}			
		});
	}
	
	public void gprsTest () {
		new GPRSTestState () {
			public void done () {
				new CommCareHomeState().start();
			}
		}.start();
	}
	
	public void adminLogin() {
		new CommCareLoginState(true) {
			public void exit() {
				new CommCareHomeState().start();
			}
		}.start();
	}
	
	public void forceSend() {
		J2MEDisplay.startStateWithLoadingScreen(new SendAllUnsentState () {
			protected SendAllUnsentController getController () {
				return new SendAllUnsentController(new CommCareHQResponder(PropertyManager._().getSingularProperty(JavaRosaPropertyRules.OPENROSA_API_LEVEL)));
			}

			public void done() {
				new CommCareHomeState().start();
			}
		});

		//assumes this will cause queued forms to be sent imminently
		//AutomatedSenderService.NotifyPending();
	}
}
