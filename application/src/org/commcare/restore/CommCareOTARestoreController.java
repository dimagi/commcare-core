/**
 * 
 */
package org.commcare.restore;

import java.util.Hashtable;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import org.commcare.model.PeriodicEvent;
import org.commcare.resources.model.CommCareOTARestoreListener;
import org.commcare.util.CommCareRestorer;
import org.commcare.util.CommCareUtil;
import org.commcare.util.time.AutoSyncEvent;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.services.storage.StorageModifiedException;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.service.transport.securehttp.AuthenticatedHttpTransportMessage;
import org.javarosa.service.transport.securehttp.DefaultHttpCredentialProvider;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;
import org.javarosa.user.model.User;

/**
 * 
 * This class is a huge quagmire of interlinking completely coupled method stacks. Rewrite to have clear
 * workflow v. functional chunks
 * @author ctsims
 * 
 * This class now handles requests for restoring, passing them into the asynchronous class
 * CommCareRestorer, acting as the listener, and updating the CommCareOTARestoreView
 *
 *@author wspride
 */
public class CommCareOTARestoreController implements HandledCommandListener, CommCareOTARestoreListener {
	
	CommCareOTACredentialEntry entry;
	CommCareOTARestoreView view;
	CommCareOTAFailView fView;
	CommCareRestorer mRestorer;
	
	CommCareOTARestoreTransitions transitions;
	
	int authAttempts = 0;
	String restoreURI;
	boolean noPartial;
	boolean isSync;
	int[] caseTallies;
	int totalItemCount = -1;
	
	HttpAuthenticator authenticator;
	boolean errorsOccurred;
	String syncToken;
	String originalRestoreURI;
	String logSubmitURI;
	String stateHash;
	
	public CommCareOTARestoreController(CommCareOTARestoreTransitions transitions, String restoreURI) {
		this(transitions, restoreURI, null);
	}
	
	public CommCareOTARestoreController(CommCareOTARestoreTransitions transitions, String restoreURI, HttpAuthenticator authenticator) {
		this(transitions, restoreURI, authenticator, false, false, null, null);
	}
				
	public CommCareOTARestoreController(CommCareOTARestoreTransitions transitions, String restoreURI,
			HttpAuthenticator authenticator, boolean isSync, boolean noPartial, String syncToken, String logSubmitURI) {

		view = new CommCareOTARestoreView(Localization.get("intro.restore"));
		view.setCommandListener(this);
		
		fView = new CommCareOTAFailView(Localization.get("intro.restore"));
		fView.setCommandListener(this);
		
		entry = new CommCareOTACredentialEntry(Localization.get("intro.restore"));
		entry.setCommandListener(this);
		
		mRestorer = new CommCareRestorer();
		
		this.transitions = transitions;
		this.restoreURI = restoreURI;
		this.authenticator = authenticator;
		this.isSync = isSync;
		this.noPartial = noPartial;
		this.syncToken = syncToken;
		this.logSubmitURI = logSubmitURI;
		
	}
	
	public void start() {
		mRestorer.initialize(this, transitions, restoreURI, authenticator, isSync, noPartial, syncToken, logSubmitURI);
	}
	
	protected String getCacheRef() {
		return "jr://file/commcare_ota_backup.xml";
	}
	

	public void _commandAction(Command c, Displayable d) {
		System.out.println("Command action, c: " + c.getLabel() + ", d: " + d.getTitle());
		if(c.equals(CommCareOTACredentialEntry.DOWNLOAD)) {
			if(userExists(entry.getUsername()) && !isSync) {
				entry.sendMessage(Localization.get("restore.user.exists"));
				return;
			}
			this.authenticator = new HttpAuthenticator(CommCareUtil.wrapCredentialProvider(new DefaultHttpCredentialProvider(entry.getUsername(), entry.getPassword())), false);
			mRestorer.initialize(this, transitions, restoreURI, authenticator, isSync, noPartial, syncToken, logSubmitURI);
			//tryDownload(getClientMessage());
		} else if(d == entry && c.equals(CommCareOTACredentialEntry.CANCEL)) {
			transitions.cancel();
		} else if(c.equals(view.FINISHED)) {
			PeriodicEvent.markTriggered(new AutoSyncEvent());
			transitions.done(errorsOccurred);
		}
		else if(c.equals(CommCareOTAFailView.DOWNLOAD)){
			mRestorer.initialize(this, transitions, restoreURI, authenticator, isSync, noPartial, syncToken, logSubmitURI);
		}
		else if(c.equals(CommCareOTAFailView.CANCEL)){
			System.out.println("entered cancel!");
			transitions.cancel();
		}
	}
	
	private boolean userExists(String username) {
		int attempts = 0;
		//An absurd number of tries
		while(attempts < 50) {
			try{
				IStorageIterator iterator = StorageManager.getStorage(User.STORAGE_KEY).iterate();
				while(iterator.hasMore()) {
					User u = (User)iterator.nextRecord();
					if(username.toLowerCase().equals(u.getUsername().toLowerCase())) {
						return true;
					}
				}
				return false;
			}
			catch(StorageModifiedException sme) {
				//storage modified while we were going through users. Try again
				attempts++;
			}
		}
		//Dunno what to do here, really, it would be crazy to gt to this point.
		//Maybe should throw an exception, actually.
		Logger.log("restore", "Could not look through User list to determine if user " + username + " exists.");
		return false;
	}

	public void commandAction(Command c, Displayable d) {
		CrashHandler.commandAction(this,c,d);
	}
	
	public Hashtable<String, Integer> getCaseTallies() {
		return mRestorer.getCaseTallies();
	}
	
	private void done() {
		view.setFinished();
		view.addToMessage(Localization.get("restore.key.continue"));
	}
	
	private void doneFail(String msg) {
		if (msg != null) {
			Logger.log("restore", "fatal error: " + msg);
		}
		if (this.isSync) {
			done();
			errorsOccurred = true;
		}
	}
	
	private void fail(String message, Exception e, String logmsg) {
		view.addToMessage(message);
		
		if (logmsg == null) {
			logmsg = (e != null ? WrappedException.printException(e) : "no message");
		}
		Logger.log("restore", "fatal error: " + logmsg);
		
		if (e != null) {
			e.printStackTrace();
		}
		
		//Retry/Cancel from scratch or by 
	}
	
	public void setView(){
		J2MEDisplay.setView(view);
	}
	
	public void getCredentials() {
		J2MEDisplay.setView(entry);
	}
	
	public void setFailView(){
		J2MEDisplay.setView(fView);
	}
	
	public void setFailView(String msg){
		fView.setMessage(msg);
		setFailView();
	}

	public void onSuccess() {
		view.setFinished();
		view.addToMessage(Localization.get("restore.key.continue"));
	}

	public void onUpdate(int numberCompleted) {
		view.updateProgress(numberCompleted);
	}

	public void statusUpdate(int statusNumber) {
		switch(statusNumber){
			case CommCareOTARestoreListener.REGULAR_START:
				entry.sendMessage("");
				return;
			case CommCareOTARestoreListener.BYPASS_START:	
				view.addToMessage(Localization.get("restore.bypass.start", new String [] {mRestorer.getBypassRef().getLocalURI()}));
			case CommCareOTARestoreListener.BYPASS_CLEAN:	
				view.addToMessage(Localization.get("restore.bypass.clean"));
			case CommCareOTARestoreListener.BYPASS_CLEAN_SUCCESS:	
				view.addToMessage(Localization.get("restore.bypass.clean.success"));
				return;
			case CommCareOTARestoreListener.BYPASS_CLEANFAIL:	
				view.addToMessage(Localization.get("restore.bypass.cleanfail", new String[] {mRestorer.getBypassRef().getLocalURI()}));
			case CommCareOTARestoreListener.BYPASS_FAIL:	
				view.addToMessage(Localization.get("restore.bypass.fail"));
				entry.sendMessage(Localization.get("restore.bypass.instructions"));    
			case CommCareOTARestoreListener.RESTORE_BAD_CREDENTIALS:	
				view.addToMessage(Localization.get("restore.badcredentials"));
				entry.sendMessage(Localization.get("restore.badcredentials"));
			case CommCareOTARestoreListener.RESTORE_CONNECTION_FAILED:	
				//Connection failed; could be for any number of reasons, add to message and proceed
				view.addToMessage(Localization.get("restore.message.connection.failed"));
			case CommCareOTARestoreListener.RESTORE_BAD_DB:	
				entry.sendMessage(Localization.get("restore.bad.db"));
				view.setMessage(Localization.get("restore.bad.db"));
			case CommCareOTARestoreListener.RESTORE_DB_BUSY:	
				view.addToMessage("We're still busy loading your cases and follow-ups. Try again in five minutes.");
				entry.sendMessage("We're still busy loading your cases and follow-ups. Try again in five minutes.");
			case CommCareOTARestoreListener.RESTORE_CONNECTION_MADE:	
				view.addToMessage(Localization.get("restore.message.connectionmade"));
			case CommCareOTARestoreListener.RESTORE_BAD_DOWNLOAD:
				entry.sendMessage(Localization.get("restore.baddownload"));
			case CommCareOTARestoreListener.RESTORE_BAD_SERVER:
				entry.sendMessage(Localization.get("restore.badserver"));
			case CommCareOTARestoreListener.RESTORE_FAIL_OTHER:
				entry.sendMessage("");	//TODO entry.sendMessage(sent.getFailureReason());
			case CommCareOTARestoreListener.RESTORE_DOWNLOAD:
				view.addToMessage(Localization.get("restore.message.startdownload"));
			case CommCareOTARestoreListener.RESTORE_RECOVER_SEND:
				view.addToMessage(Localization.get("restore.recover.send"));
			case CommCareOTARestoreListener.RESTORE_NO_CACHE:
				view.addToMessage(Localization.get("restore.nocache"));
			case CommCareOTARestoreListener.RESTORE_DOWNLOADED:
				view.addToMessage(Localization.get("restore.downloaded"));
			case CommCareOTARestoreListener.RESTORE_NEED_CACHE:
				view.setMessage(Localization.get("restore.recover.needcache"));
			case CommCareOTARestoreListener.RESTORE_START:
				view.addToMessage(Localization.get("restore.starting"));
			case CommCareOTARestoreListener.RESTORE_RECOVERY_WIPE:
				view.addToMessage(Localization.get("restore.recovery.wipe"));
			case CommCareOTARestoreListener.RESTORE_SUCCESS:
				view.addToMessage(Localization.get("restore.success"));
			case CommCareOTARestoreListener.RESTORE_FAIL:
				view.addToMessage(Localization.get("restore.fail"));
			case CommCareOTARestoreListener.RESTORE_FAIL_PARTIAL:
				view.addToMessage(Localization.get("restore.success.partial"));// + " " + parseErrors.length); //TODO
			case CommCareOTARestoreListener.RESTORE_CONNECTION_FAIL_ENTRY:
				entry.sendMessage(Localization.get("restore.message.connection.failed"));
			default: 
		}
	}
	
	public void setTotalForms(int totalItemCount){
		this.totalItemCount = totalItemCount;
		view.setTotalItems(totalItemCount);
	}

	public void refreshView(){
		setView();
	}

	public void onFailure(String failMessage) {
		doneFail(failMessage);
	}
	
	public void promptRetry(String msg){
		setFailView(msg);
	}
}
