/**
 * 
 */
package org.commcare.applogic;

import java.util.Hashtable;

import org.commcare.api.transitions.CommCareHomeTransitions;
import org.commcare.entity.RecentFormEntity;
import org.commcare.restore.CommCareOTARestoreController;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.commcare.util.CommCareSession;
import org.commcare.util.CommCareSessionController;
import org.commcare.util.CommCareUtil;
import org.commcare.util.UserCredentialProvider;
import org.commcare.view.CommCareHomeController;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.cases.model.Case;
import org.javarosa.chsreferral.model.PatientReferral;
import org.javarosa.core.api.State;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.EntityFilter;
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
import org.javarosa.service.transport.securehttp.HttpAuthenticator;
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
				return new SendAllUnsentController(new CommCareHQResponder());
			}

			public void done() {
				new CommCareHomeState().start();
			}
		});
	}
	
	private static CommCareAlertState alertFactory (String title, String content) {
		return new CommCareAlertState(title, content) {
			public void done() {
				J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
			}
		};
	}
	
	public void serverSync () {
		J2MEDisplay.startStateWithLoadingScreen(new SendAllUnsentState () {
			protected SendAllUnsentController getController () {
				return new SendAllUnsentController(new CommCareHQResponder(), false, true);
			}

			public void done () {
				throw new RuntimeException("method not applicable");
			}
			
			public void done(boolean errorsOccurred) {
				if (errorsOccurred) {
					System.out.println("debug: server sync: errors occurred during send-all-unsent");
					J2MEDisplay.startStateWithLoadingScreen(alertFactory(
						"Failed to update",
					//	"We were unable to send your forms back to the clinic and fetch your updated follow-up list. Try again when you have better reception and/or more talk-time."
						"We were unable to send your forms back to the clinic and fetch your updated follow-up list. Try again when you have better reception. If this keeps happening, get help from your program manager."
					));
				} else {
					System.out.println("debug: server sync: send-all-unsent successful");
					
					J2MEDisplay.startStateWithLoadingScreen(new CommCareOTARestoreState (true,
							new HttpAuthenticator(new UserCredentialProvider(CommCareContext._().getUser()))) {

						public void cancel() {
							//don't think this is cancellable, since the only place you can cancel from is
							//the credentials screen, and we skip that
							throw new RuntimeException("shouldn't be cancellable");
						}

						public String restoreDetailMsg () {
							Hashtable<String, Integer> tallies = controller.getCaseTallies();
							int created = tallies.get("create").intValue();
							int updated = tallies.get("update").intValue();
							int closed = tallies.get("close").intValue();
							
							if (created + updated + closed == 0) {
								return "No new updates.";
							} else {
								String msg = "";
								if (created > 0) {
									msg += (msg.length() > 0 ? "; " : "") + created + " new follow-ups";
								}
								if (closed > 0) {
									msg += (msg.length() > 0 ? "; " : "") + closed + " follow-ups closed by clinic";
								}
								if (updated > 0) {
									msg += (msg.length() > 0 ? "; " : "") + updated + " open follow-ups updated";
								}
								return msg + ".";
							}
						}
						
						public void done(boolean errorsOccurred) {
							if (errorsOccurred) {
								System.out.println("debug: server sync: errors occurred during pull-down");
								J2MEDisplay.startStateWithLoadingScreen(alertFactory(
									"Failed to update",
//									"There was a problem and we couldn't get all of your new follow-ups from the clinic. You should try again when you have better reception and/or more talk-time."
									"There was a problem and we couldn't get your new follow-ups from the clinic. You should try again when you have better reception. If this keeps happening, get help from your program manager."
								));								
							} else {
								J2MEDisplay.startStateWithLoadingScreen(alertFactory("Update", "Update successful! " + restoreDetailMsg()));
							}
						}						
					});
				}
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
						FormDefFetcher fetcher = new FormDefFetcher(new ModelRmsRetrievalMethod(instanceID), instanceID,
								CommCareContext._().getPreloaders(), CommCareContext._().getFuncHandlers());
						JrFormEntryController controller = new JrFormEntryController(new JrFormEntryModel(fetcher.getFormDef(), true));
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
		try {
			CommCareContext._().getManager().upgrade(CommCareContext.RetrieveGlobalResourceTable(), CommCareContext.CreateTemporaryResourceTable("UPGRADGE"));
		} catch (UnfullfilledRequirementsException e) {
			J2MEDisplay.showError(null,Localization.get("commcare.noupgrade.version"));
		}
	}
	
	public void rmsdump () {
		try {
			DumpRMS.dumpRMS(CommCareContext._().getMidlet().getAppProperty("RMS-Image-Path"));
			J2MEDisplay.startStateWithLoadingScreen(alertFactory("RMS Dump", "Dump successful!"));
		} catch (Exception e) {
			Logger.exception(e);
			J2MEDisplay.startStateWithLoadingScreen(alertFactory("RMS Dump Failed!", WrappedException.printException(e)));
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
}
