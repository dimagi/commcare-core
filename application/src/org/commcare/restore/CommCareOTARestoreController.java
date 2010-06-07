/**
 * 
 */
package org.commcare.restore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import org.commcare.data.xml.DataModelPullParser;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.UserXmlParser;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.StreamUtil;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.service.transport.securehttp.AuthenticatedHttpTransportMessage;
import org.javarosa.service.transport.securehttp.DefaultHttpCredentialProvider;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;
import org.javarosa.service.transport.securehttp.HttpCredentialProvider;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportException;
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
		entry.sendMessage("");
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

	public void startRestore(InputStream input) {
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
			} else {
				view.addToMessage(Localization.get("restore.success.partial"));
				for(String s : parser.getParseErrors()) {
					view.addToMessage(s);
					done();
				}
			}
			
		} catch(IOException e) {
			fail(Localization.get("restore.fail"));
		} catch (InvalidStructureException e) {
			fail(Localization.get("restore.fail"));
		} catch (XmlPullParserException e) {
			fail(Localization.get("restore.fail"));
		}
	}
	
	private void done() {
		view.setFinished();
		view.addToMessage("Press any key to continue...");
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
			tryDownload(getClientMessage());
		} else if(d == entry && c.equals(CommCareOTACredentialEntry.CANCEL)) {
			transitions.cancel();
		} else if(c.equals(view.FINISHED)) {
			transitions.done();
		}
	}

	public void commandAction(Command c, Displayable d) {
		CrashHandler.commandAction(this,c,d);
	}
}
