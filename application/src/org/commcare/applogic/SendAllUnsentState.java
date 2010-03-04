package org.commcare.applogic;

import org.javarosa.core.api.State;
import org.javarosa.core.util.TrivialTransitions;

public abstract class SendAllUnsentState implements TrivialTransitions, State {

	public void start () {
		SendAllUnsentController controller = getController();
		controller.setTransitions(this);
		controller.start();
	}
	
	protected SendAllUnsentController getController () {
		return new SendAllUnsentController();
	}
	
}
