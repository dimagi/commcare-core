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
		controller.start();
	}
	
	protected CommCareOTARestoreController getController() {
		return new CommCareOTARestoreController(this,CommCareContext._().getOTAURL());
	}
}
