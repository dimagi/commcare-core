/**
 * 
 */
package org.commcare.applogic;

import org.commcare.api.transitions.FirstStartupTransitions;
import org.commcare.util.CommCareContext;
import org.commcare.view.FirstStartupView;
import org.javarosa.core.api.State;
import org.javarosa.j2me.view.J2MEDisplay;

/**
 * @author ctsims
 *
 */
public abstract class FirstStartupState implements State, FirstStartupTransitions {
	
	
	FirstStartupView view;
	
	public FirstStartupState() {
		view = new FirstStartupView(this);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.api.State#start()
	 */
	public void start() {
		J2MEDisplay.setView(view);
	}
	
	public void exit() {
		CommCareContext._().getMidlet().notifyDestroyed();
	}
}
