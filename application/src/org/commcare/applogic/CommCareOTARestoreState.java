/**
 * 
 */
package org.commcare.applogic;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.restore.CommCareOTARestoreController;
import org.commcare.restore.CommCareOTARestoreTransitions;
import org.commcare.util.CommCareContext;
import org.javarosa.core.api.State;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;

/**
 * @author ctsims
 *
 */
public abstract class CommCareOTARestoreState implements State, CommCareOTARestoreTransitions {

	CommCareOTARestoreController controller;

	private boolean isSync;
	private boolean noPartial;
	private HttpAuthenticator authenticator;
	
	public CommCareOTARestoreState() {
		this(false, null);
	}
	
	public CommCareOTARestoreState(boolean isSync, HttpAuthenticator authenticator) {
		this.isSync = isSync;
		this.authenticator = authenticator;
		this.noPartial = getPartialRestoreSetting();
		
		controller = getController();
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.api.State#start()
	 */
	public void start() {
		controller.start();
	}
	
	protected CommCareOTARestoreController getController() {
		return new CommCareOTARestoreController(
			this,
			CommCareContext._().getOTAURL(),
			authenticator,
			isSync,
			noPartial
		);
	}
	
	private boolean getPartialRestoreSetting () {
		throw new RuntimeException("not implemented yet");
	}
}
