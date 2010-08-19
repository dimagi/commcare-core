/**
 * 
 */
package org.commcare.restore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.UserXmlParser;
import org.commcare.xml.util.InvalidStructureException;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.services.storage.StorageModifiedException;
import org.javarosa.core.util.StreamUtil;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.log.HandledThread;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.service.transport.securehttp.AuthenticatedHttpTransportMessage;
import org.javarosa.service.transport.securehttp.DefaultHttpCredentialProvider;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportException;
import org.javarosa.user.model.User;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class CommCareOTARestoreController implements HandledCommandListener {
	
	CommCareOTACredentialEntry entry;
	CommCareOTARestoreView view;
	
	CommCareOTARestoreTransitions transitions;
	
	int authAttempts = 0;
	String restoreURI;
	
	HttpAuthenticator authenticator;
	
	public CommCareOTARestoreController(CommCareOTARestoreTransitions transitions, String restoreURI) {
		this(transitions,restoreURI,null);
	}
	
	public CommCareOTARestoreController(CommCareOTARestoreTransitions transitions, String restoreURI, HttpAuthenticator authenticator) {
		this.restoreURI = restoreURI;
		this.authenticator = authenticator;
		
		view = new CommCareOTARestoreView(Localization.get("intro.restore"));
		view.setCommandListener(this);
		
		entry = new CommCareOTACredentialEntry(Localization.get("intro.restore"));
		entry.setCommandListener(this);
		
		this.transitions = transitions;
	}
	
	public void start() {
		Reference bypassRef = getBypassRef();
		if(bypassRef != null) {
			J2MEDisplay.setView(view);
			tryBypass(bypassRef);
		} else{ 
			entry.sendMessage("");
			startOtaProcess();
		}
	}
	
	private void startOtaProcess() {
		 if(authenticator == null) {
			authAttempts = 0;
			getCredentials();
		} else {
			authAttempts = 1;
			J2MEDisplay.setView(view);
			tryDownload(AuthenticatedHttpTransportMessage.AuthenticatedHttpRequest(restoreURI, authenticator));
		}
	}
	
	private void getCredentials() {
		J2MEDisplay.setView(entry);
	}
	
	private void tryDownload(AuthenticatedHttpTransportMessage message) {
		view.addToMessage(Localization.get("restore.message.startdownload"));
		try {
			if(message.getUrl() == null) {
				fail(Localization.get("restore.noserveruri"));
				J2MEDisplay.setView(view);
				return;
			}
			AuthenticatedHttpTransportMessage sent = (AuthenticatedHttpTransportMessage)TransportService.sendBlocking(message);
			if(sent.isSuccess()) {
				view.addToMessage(Localization.get("restore.message.connectionmade"));
				try {
					downloadRemoteData(sent.getResponse());
					return;
				} catch(IOException e) {
					J2MEDisplay.setView(entry);
					entry.sendMessage(Localization.get("restore.baddownload"));
					return;
				}
			} else {
				view.addToMessage(Localization.get("restore.message.connection.failed"));
				if(sent.getResponseCode() == 401) {
					view.addToMessage(Localization.get("restore.badcredentials"));
					entry.sendMessage(Localization.get("restore.badcredentials"));
					if(authAttempts > 0) {
						authAttempts--;
						getCredentials();
					}
					return;
				} else if(sent.getResponseCode() == 404) {
					entry.sendMessage(Localization.get("restore.badserver"));
					return;
				} else {
					entry.sendMessage(sent.getFailureReason());
					return;
				}
			}
		} catch (TransportException e) {
			entry.sendMessage(Localization.get("restore.message.connection.failed"));
		}
	}
	
	/**
	 * 
	 * @param stream
	 * @throws IOException If there was a problem which resulted in the cached
	 * file being corrupted or unavailable _and_ the input stream becoming invalidated
	 * such that a retry is necessary.
	 */
	private void downloadRemoteData(InputStream stream) throws IOException {
		J2MEDisplay.setView(view);
		Reference ref;
		try {
			ref = ReferenceManager._().DeriveReference(getCacheRef());
			if(ref.isReadOnly()) {
				view.addToMessage(Localization.get("restore.nocache"));
				//TODO: ^ That.
			} else {
				OutputStream output;
				
				//We want to treat any problems dealing with the inability to 
				//download as seperate from a _failed_ download, which is 
				//what this try-catch is all about.
				try {
					if(ref.doesBinaryExist()) {
						ref.remove();
					}
					output = ref.getOutputStream();
				}
			    catch (Exception e) {
			    	noCache(stream);
			    	return;
				}
			    
			    //Now any further IOExceptions will get handled as "download failed", 
			    //rather than "couldn't attempt to download"
				StreamUtil.transfer(stream, output);
				view.addToMessage(Localization.get("restore.downloaded"));
				startRestore(ref.getStream());
			}
		} catch (InvalidReferenceException e) {
			noCache(stream);
		}

	}
	
	private void noCache(InputStream input) {
		view.addToMessage(Localization.get("restore.nocache"));
		startRestore(input);
	}

	public boolean startRestore(InputStream input) {
		J2MEDisplay.setView(view);
		view.addToMessage(Localization.get("restore.starting")); 
		try {
			DataModelPullParser parser = new DataModelPullParser(input, new TransactionParserFactory() {
	
				public TransactionParser getParser(String name, String namespace, KXmlParser parser) {
					if(name.toLowerCase().equals("case")) {
						return new CaseXmlParser(parser);
					} else if(name.toLowerCase().equals("registration")) {
						return new UserXmlParser(parser);
					}
					return null;
				}
				
			});
			if(parser.parse()) {
				view.addToMessage(Localization.get("restore.success"));
				done();
				return true;
			} else {
				String[] errors = parser.getParseErrors();
				view.addToMessage(Localization.get("restore.success.partial") + " " + errors.length);
				for(String s : parser.getParseErrors()) {
					Logger.log("restore", s);
				}
				done();
				return true;
			}
			
		} catch(IOException e) {
			fail(Localization.get("restore.fail"));
			e.printStackTrace();
			return false;
		} catch (InvalidStructureException e) {
			fail(Localization.get("restore.fail"));
			e.printStackTrace();
			return false;
		} catch (XmlPullParserException e) {
			fail(Localization.get("restore.fail"));
			e.printStackTrace();
			return false;
		} catch (UnfullfilledRequirementsException e) {
			fail(Localization.get("restore.fail"));
			e.printStackTrace();
			return false;
		}
	}
	
	private void done() {
		view.setFinished();
		view.addToMessage(Localization.get("restore.key.continue"));
	}
	
	private void fail(String message) {
		view.addToMessage(message);
		//Retry/Cancel from scratch or by 
	}

	protected String getCacheRef() {
		return "jr://file/commcare_ota_backup.xml";
	}
	
	private AuthenticatedHttpTransportMessage getClientMessage() {
		AuthenticatedHttpTransportMessage message = AuthenticatedHttpTransportMessage.AuthenticatedHttpRequest(restoreURI, 
				new HttpAuthenticator(new DefaultHttpCredentialProvider(entry.getUsername(), entry.getPassword()), false));
		return message;
	}

	public void _commandAction(Command c, Displayable d) {
		if(c.equals(CommCareOTACredentialEntry.DOWNLOAD)) {
			if(userExists(entry.getUsername())) {
				entry.sendMessage(Localization.get("restore.user.exists"));
				return;
			}
			
			tryDownload(getClientMessage());
		} else if(d == entry && c.equals(CommCareOTACredentialEntry.CANCEL)) {
			transitions.cancel();
		} else if(c.equals(view.FINISHED)) {
			transitions.done();
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
	
	/**
	 * 
	 * @return Null if the bypass file doesn't exist or couldn't be resolved. A Reference to the bypass
	 * file if one appears to exist.
	 */
	protected Reference getBypassRef() {
		try {
			String bypassRef = PropertyManager._().getSingularProperty(CommCareProperties.OTA_RESTORE_OFFLINE);
			if(bypassRef == null || bypassRef == "") {
				return null;
			}
		
			Reference bypass = ReferenceManager._().DeriveReference(bypassRef);
			if(bypass == null || !bypass.doesBinaryExist()) {
				return null;
			}
			return bypass;
		} catch(Exception e){
			e.printStackTrace();
			//It would be absurdly stupid if we couldn't OTA restore because of an error here
			return null;
		}
		
	}
	
	public void tryBypass(final Reference bypass) {
		//Need to launch the bypass attempt in a thread.
		
		HandledThread t = new HandledThread() {
			public void _run() {
				view.addToMessage(Localization.get("restore.bypass.start", new String [] {bypass.getLocalURI()}));
				
				try {
					Logger.log("restore", "starting bypass restore attempt with file: " + bypass.getLocalURI());
					InputStream restoreStream = bypass.getStream();
					if(startRestore(restoreStream)) {
						try {
							//Success! Try to wipe the local file and then let the UI handle the rest.
							restoreStream.close();
							if(!bypass.isReadOnly()) {
								view.addToMessage(Localization.get("restore.bypass.clean"));
								bypass.remove();
								view.addToMessage(Localization.get("restore.bypass.clean.success"));
							}
						} catch (IOException e) {
							//Even if we fail to delete the local file, it's mostly fine. Jut let the user know
							e.printStackTrace();
							view.addToMessage(Localization.get("restore.bypass.cleanfail", new String[] {bypass.getLocalURI()}));
						}
						Logger.log("restore", "bypass restore succeeded");
						return;
					}
				} catch(IOException e) {
					//Couldn't open a stream to the restore file, we'll need to dump out to
					//OTA
					e.printStackTrace();
				}
				
				//Something bad about the restore file. 
				//Skip it and dump back to OTA Restore
				
				Logger.log("restore", "bypass restore failed, falling back to OTA");
				view.addToMessage(Localization.get("restore.bypass.fail"));
				
				entry.sendMessage(Localization.get("restore.bypass.instructions"));
				startOtaProcess();
			}
		};
		t.start();
	}
}
