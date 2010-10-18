package org.commcare.applogic;

import java.util.Hashtable;

import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.commcare.util.UserCredentialProvider;
import org.javarosa.core.api.State;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;

public abstract class ServerSyncState implements State {

	String sendFailMsg = "We were unable to send your forms back to the clinic and fetch your updated follow-up list. Try again when you have better reception. If this keeps happening, get help from your program manager.";
	String pullFailMsg = "There was a problem and we couldn't get your new follow-ups from the clinic. You should try again when you have better reception. If this keeps happening, get help from your program manager.";
	
	SendAllUnsentState send;
	CommCareOTARestoreState pull;
	
	public ServerSyncState () {
		send = new SendAllUnsentState () {
			protected SendAllUnsentController getController () {
				return new SendAllUnsentController(new CommCareHQResponder(), false, true);
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
					
		HttpAuthenticator auth = new HttpAuthenticator(new UserCredentialProvider(CommCareContext._().getUser()));
		pull = new CommCareOTARestoreState (true, auth) {
			public void cancel() {
				//don't think this is cancellable, since the only place you can cancel from is
				//the credentials screen, and we skip that
				throw new RuntimeException("shouldn't be cancellable");
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
