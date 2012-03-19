/**
 * 
 */
package org.commcare.applogic;

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import org.javarosa.core.services.Logger;
import org.javarosa.core.util.TrivialTransitionsWithErrors;
import org.javarosa.formmanager.view.transport.MultiSubmitStatusScreen;
import org.javarosa.formmanager.view.transport.TransportResponseProcessor;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.log.HandledThread;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportException;

/**
 * @author ctsims
 *
 */
public class SendAllUnsentController implements HandledCommandListener {

	private TrivialTransitionsWithErrors transitions;
	private MultiSubmitStatusScreen screen;
	private boolean shortcircuit;
	
	public SendAllUnsentController() {
		this(null);
	}
	
 	public SendAllUnsentController(TransportResponseProcessor responder) {
 		this(responder, true, false, null);
 	}
 		
 	public SendAllUnsentController(TransportResponseProcessor responder, boolean silenceable, boolean shortcircuit, String cancelText) {
 		screen = new MultiSubmitStatusScreen(this, responder, silenceable, cancelText);
 		this.shortcircuit = shortcircuit;
 	}
	
	public void setTransitions (TrivialTransitionsWithErrors transitions) {
		this.transitions = transitions;
	}

	public void start() {
		//This block is all vaguely hacky. We should reengineer this process from scratch.
		Vector messages = TransportService.getCachedMessages();
		String[] ids = new String[messages.size()];

		if (ids.length == 0 && this.shortcircuit) {
			transitions.done(false);
			return;
		}
		
		for (int i = 0; i < ids.length; ++i) {
			ids[i] = ((TransportMessage) messages.elementAt(i)).getCacheIdentifier();
		}

		screen.reinit(ids);
		J2MEDisplay.setView(screen);
		//end dumb stuff
		new HandledThread(new Runnable() {
			public void run() {
				//For some reason the transport service doesn't do this in a thread, despite
				//implementing the observer pattern. Until that changes, we'll do it here.
				try {
					TransportService.sendCached(screen);
				} catch (TransportException e) {
					Logger.exception("SendAllUnsentController.start", e);
					screen.receiveError(e.getMessage());
					transitions.done(true);
				}
			}
		}).start();
	}

	public void commandAction(Command c, Displayable d) {
		CrashHandler.commandAction(this, c, d);
	}  

	public void _commandAction(Command c, Displayable d) {
		transitions.done(screen.hasErrors());
	}

}
