/**
 *
 */
package org.commcare.applogic;

import org.commcare.api.transitions.CommCareHomeTransitions;
import org.commcare.cases.model.Case;
import org.commcare.core.properties.CommCareProperties;
import org.commcare.entity.RecentFormEntity;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.commcare.util.CommCareSession;
import org.commcare.util.CommCareSessionController;
import org.commcare.util.CommCareUtil;
import org.commcare.view.CommCareHomeController;
import org.javarosa.core.api.State;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.MemoryUtils;
import org.javarosa.formmanager.api.FormEntryState;
import org.javarosa.formmanager.api.JrFormEntryController;
import org.javarosa.formmanager.api.JrFormEntryModel;
import org.javarosa.formmanager.utility.FormDefFetcher;
import org.javarosa.formmanager.utility.ModelRmsRetrievalMethod;
import org.javarosa.formmanager.view.chatterbox.Chatterbox;
import org.javarosa.j2me.log.viewer.LogViewerState;
import org.javarosa.j2me.util.DumpRMS;
import org.javarosa.j2me.util.GPRSTestState;
import org.javarosa.j2me.util.PermissionsTestState;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.j2me.view.ProgressIndicator;
import org.javarosa.log.activity.DeviceReportState;
import org.javarosa.log.properties.LogPropertyRules;
import org.javarosa.log.util.LogReportUtils;
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
        MemoryUtils.printMemoryTest("Home Screen");
        sessionController = new CommCareSessionController(new CommCareSession(CommCareContext._().getManager()));
        CommCareHomeController home = new CommCareHomeController(CommCareContext._().getManager().getCurrentProfile(), sessionController);
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
        }, new ProgressIndicator() {

            public double getProgress() {
                return -1;
            }

            public String getCurrentLoadingStatus() {
                return "Purging local data...";
            }

            public int getIndicatorsProvided() {
                return ProgressIndicator.INDICATOR_STATUS;
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

            public void commitSyncToken(String restoreID) {
                //Since we're restoring users with no specific start point, they should be assigned
                //the sync token when their user model is created.
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
    }

    public void newUser() {
        J2MEDisplay.startStateWithLoadingScreen(new CommCareAddUserState(
                !CommCareProperties.USER_REG_SKIP.equals(PropertyManager._().getSingularProperty(CommCareProperties.USER_REG_TYPE)),
                PropertyManager._().getSingularProperty(JavaRosaPropertyRules.OPENROSA_API_LEVEL)));
    }

    public void editUsers() {
        //2012-10-22 - ctsims - Disabling this for now unless you're in completely offline user mode.
        //There's no way to do it without an intermediate authentication otherwise.

        if(CommCareProperties.USER_REG_SKIP.equals(PropertyManager._().getSingularProperty(CommCareProperties.USER_REG_TYPE))) {
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
        } else {
            J2MEDisplay.showError("Can't edit users", "User edit is disabled when using a server. Please edit the user online.");
        }
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
                        FormDefFetcher fetcher = new FormDefFetcher(new ModelRmsRetrievalMethod(instanceID), instanceID, CommCareContext._().getPreloaders(), CommCareContext._().getFuncHandlers(), new InstanceInitializationFactory());
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
        J2MEDisplay.startStateWithLoadingScreen(new CommCareUpgradeState(true) {
            public void done() {
                CommCareUtil.launchHomeState();
            }
        });
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

            public boolean submitSupported() {
                return true;
            }

            /**
             * happens in its own thread
             */
            public void submit() {
                this.append("Attempting to submit logs to HQ...", true);
                DeviceReportState logSubmit = new DeviceReportState(LogReportUtils.REPORT_FORMAT_FULL) {

                    public String getDestURL() {
                        String url = PropertyManager._().getSingularProperty(LogPropertyRules.LOG_SUBMIT_URL);
                        if(url == null) {
                            url = CommCareContext._().getSubmitURL();
                        }
                        return url;
                    }

                    public void done() {
                        String localFile = null;
                        //See if we dumped to a file
                        if(fileNameWrittenTo != null) {
                            try {
                                localFile = ReferenceManager._().DeriveReference(fileNameWrittenTo).getLocalURI();
                                append("Dumped logs onto DeviceSD card due to network difficulties. Log is at: " + localFile, false);
                            } catch(Exception e){
                                //Guess not...
                            }
                        }

                        if(errors == null || errors.size() != 0) {
                            append("Error while submitting logs!", false);
                            if(errors != null ) {
                                for(int i = 0 ; i < errors.size() ; ++i ){
                                    try {
                                        append("[" + errors.elementAt(i)[0] + "] - " + errors.elementAt(i)[1], false);
                                    } catch(Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } else {
                            if(localFile == null) {
                                append("Logs submitted", false);
                            }
                        }
                    }
                };

                logSubmit.start();
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

    public void permissionsTest() {
        new PermissionsTestState () {
            public void done () {
                new CommCareHomeState().start();
            }
        }.start();
    }
}
