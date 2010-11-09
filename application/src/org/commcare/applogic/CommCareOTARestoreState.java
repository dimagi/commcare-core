/**
 * 
 */
package org.commcare.applogic;

import org.commcare.restore.CommCareOTARestoreController;
import org.commcare.restore.CommCareOTARestoreTransitions;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.api.State;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;

/**
 * @author ctsims
 *
 */
public abstract class CommCareOTARestoreState implements State, CommCareOTARestoreTransitions {

	CommCareOTARestoreController controller;

	private boolean isSync;
	private boolean partial;
	private HttpAuthenticator authenticator;
	
	public CommCareOTARestoreState() {
		this(false, null);
	}
	
	public CommCareOTARestoreState(boolean isSync, HttpAuthenticator authenticator) {
		this.isSync = isSync;
		this.authenticator = authenticator;
		this.partial = getPartialRestoreSetting();
		
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
			!partial
		);
	}
	
	private boolean getPartialRestoreSetting () {
		return CommCareUtil.partialRestoreEnabled();
	}
}
