/**
 * 
 */
package org.commcare.applogic;

import org.commcare.api.transitions.SuiteTransitions;
import org.commcare.suite.model.Suite;
import org.commcare.view.CommCareSuiteController;
import org.javarosa.core.api.State;

/**
 * @author ctsims
 *
 */
public abstract class SuiteHomeState implements SuiteTransitions, State {
	
	CommCareSuiteController controller;
	
	public SuiteHomeState(Suite suite) {
		this.controller = getController(suite);
	}
	
	public CommCareSuiteController getController(Suite suite) {
		return new CommCareSuiteController(suite);
	}
	public void start() {
		controller.setTransitions(this);
		controller.start();
	}
}
