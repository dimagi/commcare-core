package org.commcare.applogic;

import org.commcare.util.CommCareHQResponder;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.formmanager.api.CompletedFormOptionsState;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.services.transport.TransportMessage;

public abstract class CommCarePostFormEntryState extends CompletedFormOptionsState {

	public CommCarePostFormEntryState (TransportMessage message) {
		super(message);
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.formmanager.api.transitions.CompletedFormOptionsStateTransitions#sendData(org.javarosa.core.model.instance.FormInstance)
	 */
	public void sendData(TransportMessage message) {
		
		// The message needs to already be on the queue at this point, so we'll actually just trigger sending all unsent (rather than
		// just trying to send the individual form)
		
		J2MEDisplay.startStateWithLoadingScreen(new SendAllUnsentState () {
			protected SendAllUnsentController getController () {
				return new SendAllUnsentController(new CommCareHQResponder(PropertyManager._().getSingularProperty(JavaRosaPropertyRules.OPENROSA_API_LEVEL)));
			}

			public void done() {
				new CommCareHomeState().start();
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.javarosa.formmanager.api.transitions.CompletedFormOptionsStateTransitions#skipSend()
	 */
	public void skipSend(TransportMessage message) {
		// We're now relying on the form processor to have cached the message already, since otherwise
		// we may end up processing but not caching/sending the form. 
		Logger.log("transport", "Defer[" + message.getCacheIdentifier() + "]");
		goHome();
	}
	
	public abstract void goHome();
}
