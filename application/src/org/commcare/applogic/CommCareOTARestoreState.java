/**
 * 
 */
package org.commcare.applogic;

import java.io.IOException;

import org.commcare.restore.CommCareOTARestoreController;
import org.commcare.restore.CommCareOTARestoreTransitions;
import org.javarosa.core.api.State;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.service.transport.securehttp.AuthenticatedHttpTransportMessage;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;

/**
 * @author ctsims
 *
 */
public abstract class CommCareOTARestoreState implements State, CommCareOTARestoreTransitions {

	CommCareOTARestoreController controller;
	
	public CommCareOTARestoreState() {
		controller = getController();
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.api.State#start()
	 */
	public void start() {
		//controller.startFromEntry();
		controller.startFromTransport();
	}
	
	protected CommCareOTARestoreController getController() {
		return new CommCareOTARestoreController(this) {
			protected AuthenticatedHttpTransportMessage getCustomMessage() {
				AuthenticatedHttpTransportMessage message = new AuthenticatedHttpTransportMessage("http://dev.commcarehq.org/releasemanager/digest_test",
						new HttpAuthenticator() {

					protected String getPassword() {
						return "test";
					}

					protected String getUsername() {
						return "wrong";
					}
					
				});
				return message;
			}
		};
	}
}
