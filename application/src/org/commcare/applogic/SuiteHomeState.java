/**
 * 
 */
package org.commcare.applogic;

import org.commcare.api.transitions.SuiteTransitions;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.view.CommCareSuiteController;
import org.javarosa.core.api.State;

/**
 * @author ctsims
 *
 */
public abstract class SuiteHomeState implements SuiteTransitions, State {
	
	private CommCareSuiteController controller;
	
	public SuiteHomeState(Suite suite, Menu m) {
		this.controller = getController(suite, m);
	}
	
	public CommCareSuiteController getController(Suite suite, Menu m) {
		return new CommCareSuiteController(suite, m);
	}
	public void start() {
		controller.setTransitions(this);
		controller.start();
	}
}
