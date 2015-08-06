/**
 *
 */
package org.commcare.applogic;

import org.commcare.api.transitions.MenuTransitions;
import org.commcare.suite.model.Menu;
import org.commcare.util.CommCareSessionController;
import org.commcare.view.CommCareSuiteController;
import org.javarosa.core.api.State;

/**
 * @author ctsims
 *
 */
public abstract class MenuHomeState implements MenuTransitions, State {

    private CommCareSuiteController controller;

    public MenuHomeState(CommCareSessionController controller, Menu m) {
        this.controller = getController(controller, m);
    }

    public CommCareSuiteController getController(CommCareSessionController controller, Menu m) {
        return new CommCareSuiteController(controller, m);
    }
    public void start() {
        controller.setTransitions(this);
        controller.start();
    }
}
