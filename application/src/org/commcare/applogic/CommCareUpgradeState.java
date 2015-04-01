/**
 *
 */
package org.commcare.applogic;

import java.util.Vector;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.TableStateListener;
import org.commcare.resources.model.UnreliableSourceException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareInitializer;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.InitializationListener;
import org.commcare.util.YesNoListener;
import org.commcare.view.CommCareStartupInteraction;
import org.javarosa.core.api.State;
import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

import de.enough.polish.util.StreamUtil;

/**
 * @author ctsims
 *
 */
public abstract class CommCareUpgradeState implements State, TrivialTransitions {

    public static final String UPGRADE_TABLE_NAME = "UPGRADGE";
    public static final String RECOVERY_TABLE_NAME = "RECOVERY";
    boolean interactive = false;
    int networkRetries = -1;

    public CommCareUpgradeState(boolean interactive) {
        this(interactive, getRetryAttempts());
    }

    private static int getRetryAttempts() {
        String retryAttempts = PropertyManager._().getSingularProperty(CommCareProperties.INSTALL_RETRY_ATTEMPTS);
        if(retryAttempts == null ) { return -1; }
        try {
            return Integer.parseInt(retryAttempts);
        } catch(NumberFormatException e) {
            Logger.log("upgrade", "Bad retry attempt config set: \"" + retryAttempts + "\"");
            return -1;
        }
    }

    public CommCareUpgradeState(boolean interactive, int networkRetries) {
        this.interactive = interactive;
        this.networkRetries = networkRetries;
    }

    public void start() {
        final CommCareStartupInteraction interaction  = new CommCareStartupInteraction("CommCare is checking for updates....");
        J2MEDisplay.setView(interaction);

        CommCareInitializer upgradeInitializer = new CommCareInitializer() {
            ResourceTable upgrade = CommCareContext.CreateTemporaryResourceTable(UPGRADE_TABLE_NAME);
            ResourceTable recovery = CommCareContext.CreateTemporaryResourceTable(RECOVERY_TABLE_NAME);

            protected boolean runWrapper() throws UnfullfilledRequirementsException {

                if(networkRetries != -1) {
                    upgrade.setNumberOfRetries(networkRetries);
                }

                ResourceTable global = CommCareContext.RetrieveGlobalResourceTable();

                if(global.getTableReadiness() != ResourceTable.RESOURCE_TABLE_INSTALLED) {
                    //TODO: Recover/repair
                }

                boolean staged = false;

                while(!staged) {
                    try {
                        CommCareContext._().getManager().stageUpgradeTable(CommCareContext.RetrieveGlobalResourceTable(), upgrade, recovery, false);
                        interaction.updateProgess(20);
                        staged = true;
                    } catch (UnresolvedResourceException e) {
                        Logger.log("upgrade", "Error locating upgrade profile: " + e.getMessage());

                        if(interactive) {
                            if(blockForResponse("Couldn't find the update profile, do you want to try again?")) {
                                //loop here
                            } else {
                                return false;
                            }
                        } else{
                            return false;
                        }
                    }
                }

                Resource updateProfile = upgrade.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);

                Resource currentProfile = global.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);

                if(!(updateProfile.getVersion() > currentProfile.getVersion())){
                    if(interactive) {
                        blockForResponse("CommCare is up to date!", false);
                        return false;
                    } else {
                        return true;
                    }
                }
                if(interactive) {
                    if(!blockForResponse("Upgrade is Available! Do you want to start the update?")) {
                        return true;
                    }
                }



                setMessage(Localization.get("update.header"));

                TableStateListener upgradeListener = new TableStateListener() {

                    public final static int INSTALL_SCORE = 5;
                    public void resourceStateUpdated(ResourceTable table) {
                        int score = 0;
                        int max = 0;
                        Vector<Resource> resources = CommCarePlatform.getResourceListFromProfile(table);
                        max = resources.size() * INSTALL_SCORE;

                        if(max <= INSTALL_SCORE*2) {
                            //We'll have at least two resources when we jump in, so dont' bother updating
                            //until we have more
                            return;
                        }

                        for(Resource r : resources) {
                            switch(r.getStatus()) {
                            case Resource.RESOURCE_STATUS_UPGRADE:
                                score += INSTALL_SCORE;
                                break;
                            case Resource.RESOURCE_STATUS_INSTALLED:
                                score += INSTALL_SCORE;
                                break;
                            default:
                                score += 1;
                                break;
                            }
                        }
                        interaction.updateProgess(20 + (int)Math.ceil(65 * (score * 1.0 / max)));
                    }


                    public void incrementProgress(int complete, int total) {

                    }

                };

                TableStateListener globalListener = new TableStateListener() {

                    public void resourceStateUpdated(ResourceTable table) {

                    }

                    public void incrementProgress(int complete, int total) {

                    }

                };

                upgrade.setStateListener(upgradeListener);
                global.setStateListener(globalListener);

                //We're gonna optionally allow the user to continue the install from this resource table in
                //interactive mode
                boolean upgradeAttemptPending = true;

                while(upgradeAttemptPending) {
                    //We're trying once, so don't try again unless requested
                    upgradeAttemptPending = false;

                    try {
                        CommCareContext._().getManager().upgrade(global, upgrade, recovery);
                    } catch(UnreliableSourceException e) {
                        //We simply can't retrieve all of the resources that we're looking for.

                        //If interactive, give them the option of trying again.
                        if(interactive) {
                            if(blockForResponse(Localization.get("update.fail.network.retry"), true)) {
                                //Give it another shot!
                                setMessage(Localization.get("update.retrying"));
                                //TODO: Crank up the effort on the retries?
                                upgradeAttemptPending = true;
                                continue;
                            }
                        } else {
                            //If it's not interactive, just notify the user of failure
                            blockForResponse(Localization.get("update.fail.network"), false);
                        }

                        logFailure();
                        CommCareUpgradeState.this.done();
                        return false;
                    } catch (UnresolvedResourceException e) {
                        //Generic problem with the upgrade. Inform and return.
                        this.fail(e);
                        return false;
                    }
                }



                interaction.updateProgess(95);

                blockForResponse("CommCare Updated!", false);
                return true;
            }

            private void logFailure() {
                //Can't (or won't) keep trying.
                String logMsg = "Upgrade attempt unsuccesful. Probably due to network. ";

                //Count resources
                Vector<Resource> resources = CommCarePlatform.getResourceListFromProfile(upgrade);
                int downloaded = 0;

                for(Resource r : resources ){
                    if(r.getStatus() == Resource.RESOURCE_STATUS_UPGRADE || r.getStatus() == Resource.RESOURCE_STATUS_INSTALLED) {
                        downloaded++;
                    }
                }
                logMsg += downloaded + " of " + resources.size() + " resources were succesfully fetched/installed";

                Logger.log("upgrade", logMsg);
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

            protected void askForResponse(String message, YesNoListener yesNoListener, boolean yesNo, String left, String right) {
                if(yesNo) {
                    interaction.AskYesNo(message,yesNoListener, left, right);
                } else {
                    interaction.PromptResponse(message, yesNoListener);
                }
            }

            protected void fail(Exception e) {
                //Botched! For any number of reasons. However, let's be sure to roll back
                //any changes, if they happened.
                upgrade.clear();


                String message;
                //Note why it failed, since we don't know what this is
                Logger.exception(e);
                message = Localization.get("update.fail.generic");
                blockForResponse(message, false);
                CommCareUpgradeState.this.done();
            }
        };

        upgradeInitializer.initialize(new InitializationListener() {

            public void onSuccess() {
                CommCareUpgradeState.this.done();
            }

            public void onFailure() {
                CommCareUpgradeState.this.done();
            }

        });
    }

    public abstract void done();

}
