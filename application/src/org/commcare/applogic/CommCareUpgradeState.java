/**
 * 
 */
package org.commcare.applogic;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareInitializer;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareUtil;
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
	
	boolean interactiveIfNoUpdate = false;
	
	public CommCareUpgradeState(boolean interactiveIfNoUpdate) {
		this.interactiveIfNoUpdate = interactiveIfNoUpdate;
	}

	public void start() {
		final CommCareStartupInteraction interaction  = new CommCareStartupInteraction("CommCare is checking for updates....");
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
					if(interactiveIfNoUpdate) {
						blockForResponse("CommCare is up to date!", false);
						return false;
					} else {
						return true;
					}
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

}
