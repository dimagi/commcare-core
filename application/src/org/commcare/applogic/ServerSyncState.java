package org.commcare.applogic;

import java.util.Hashtable;

import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.api.State;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;
import org.javarosa.service.transport.securehttp.HttpCredentialProvider;
import org.javarosa.user.model.User;

public abstract class ServerSyncState implements State {

	//TODO: localize me
	String sendFailMsg = "We were unable to send your forms back to the clinic and fetch your updated follow-up list. Try again when you have better reception.";
	String pullFailMsg = "There was a problem and we couldn't get your new follow-ups from the clinic. Try again in five minutes. If it still doesn't work, try again when you have better reception.";
	
	SendAllUnsentState send;
	CommCareOTARestoreState pull;
	
	public ServerSyncState () {
		this(CommCareContext._().getCurrentUserCredentials());
	}
	
	public ServerSyncState (HttpCredentialProvider currentUserCredentials) {
		send = new SendAllUnsentState () {
			protected SendAllUnsentController getController () {
				return new SendAllUnsentController(new CommCareHQResponder(PropertyManager._().getSingularProperty(JavaRosaPropertyRules.OPENROSA_API_LEVEL)), false, true);
			}

			public void done () {
				throw new RuntimeException("method not applicable");
			}
			
			public void done(boolean errorsOccurred) {
				if (errorsOccurred) {
					System.out.println("debug: server sync: errors occurred during send-all-unsent");
					onError(sendFailMsg);
				} else {
					System.out.println("debug: server sync: send-all-unsent successful");
					launchPull();
				}
			}
		};
		
		String syncToken = null;
		User u = CommCareContext._().getUser();
		if(u != null) {
			syncToken = u.getLastSyncToken();
		}
					
		HttpAuthenticator auth = new HttpAuthenticator(CommCareUtil.wrapCredentialProvider(currentUserCredentials));
		pull = new CommCareOTARestoreState (syncToken, auth) {
			public void cancel() {
				//when your credentials have changed, the ota restore credentials screen will pop up, so we
				//do need to support canceling here.
				ServerSyncState.this.onError("Restore Cancelled");
			}
			
			public void commitSyncToken(String token) {
				if(token != null) {
					User u = CommCareContext._().getUser();
					u.setLastSyncToken(token);
					try {
						StorageManager.getStorage(User.STORAGE_KEY).write(u);
					} catch (StorageFullException e) {
						Logger.die("sync", e);
					}
				}
			}
			
			public void done(boolean errorsOccurred) {
				if (errorsOccurred) {
					System.out.println("debug: server sync: errors occurred during pull-down");
					onError(pullFailMsg);
				} else {
					onSuccess("Update successful! " + restoreDetailMsg(controller.getCaseTallies()));
				}
			}						
		};
	}
	
	public void start() {
		//This involves the global context, and thus should really be occurring in the constructor, but
		//also takes a long time, and thus is more important to not occur until starting. 
		CommCareContext._().purgeScheduler(true);
		
		send.start();
	}
	
	public void launchPull() {
		J2MEDisplay.startStateWithLoadingScreen(pull);
	}
	
	public abstract void onError (String detailMsg);
	public abstract void onSuccess (String detailMsg);
		
	//TODO: customize me
	public String restoreDetailMsg (Hashtable<String, Integer> tallies) {
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
	
}
