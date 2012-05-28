/**
 * 
 */
package org.commcare.applogic;

import java.util.Vector;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.TableStateListener;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareInitializer;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.InitializationListener;
import org.commcare.util.YesNoListener;
import org.commcare.view.CommCareStartupInteraction;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.api.State;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.j2me.view.J2MEDisplay;

/**
 * @author ctsims
 *
 */
public abstract class CommCareUpgradeState implements State, TrivialTransitions {
	
	public static final String UPGRADE_TABLE_NAME = "UPGRADGE";
	boolean interactive = false;
	
	public CommCareUpgradeState(boolean interactive) {
		this.interactive = interactive;
	}

	public void start() {
		final CommCareStartupInteraction interaction  = new CommCareStartupInteraction("CommCare is checking for updates....");
		J2MEDisplay.setView(interaction);
		
		CommCareInitializer upgradeInitializer = new CommCareInitializer() {
			ResourceTable upgrade = CommCareContext.CreateTemporaryResourceTable(UPGRADE_TABLE_NAME);

			protected boolean runWrapper() throws UnfullfilledRequirementsException {
				
				ResourceTable global = CommCareContext.RetrieveGlobalResourceTable();
				
				boolean staged = false;
				
				while(!staged) {
					try {
						CommCareContext._().getManager().stageUpgradeTable(CommCareContext.RetrieveGlobalResourceTable(), upgrade);
						interaction.updateProgess(20);
						staged = true;
					} catch (StorageFullException e) {
						Logger.die("Upgrade", e);
					} catch (UnresolvedResourceException e) {
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
				
				setMessage("Updating Installation...");
				
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
				try {
					CommCareContext._().getManager().upgrade(global, upgrade);
				} catch (UnresolvedResourceException e) {
					this.fail(e);
				}
				interaction.updateProgess(95);
				
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
				//Botched! For any number of reasons. However, let's be sure to roll back
				//any changes, if they happened.
				upgrade.clear();
				
				//Now note why this failed.
				Logger.exception(e);
				blockForResponse("An error occured during the upgrade!", false);
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
