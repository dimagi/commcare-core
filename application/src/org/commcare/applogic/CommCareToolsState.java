/**
 *
 */
package org.commcare.applogic;

import org.commcare.util.CommCareUtil;
import org.commcare.view.CommCareListView;
import org.javarosa.core.api.State;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.j2me.util.GPRSTestState;
import org.javarosa.j2me.util.PermissionsTestState;
import org.javarosa.j2me.view.J2MEDisplay;

import de.enough.polish.ui.ChoiceItem;
import de.enough.polish.ui.Command;
import de.enough.polish.ui.CommandListener;
import de.enough.polish.ui.Displayable;
import de.enough.polish.ui.List;

/**
 * @author ctsims
 *
 */
public abstract class CommCareToolsState implements State, CommandListener, TrivialTransitions {

    ChoiceItem cUpdates;
    ChoiceItem cNetwork;
    ChoiceItem cPermissions;
    ChoiceItem cMediaTest;

    Command back;
    CommCareListView view;

    public CommCareToolsState() {
        cUpdates = new ChoiceItem(Localization.get("home.updates"), null, List.IMPLICIT);
        cNetwork = new ChoiceItem(Localization.get("commcare.tools.network"), null, List.IMPLICIT);
        cPermissions = new ChoiceItem(Localization.get("commcare.tools.permissions"), null, List.IMPLICIT);
        cMediaTest = new ChoiceItem(Localization.get("commcare.tools.validate"), null, List.IMPLICIT);

        back = new Command(Localization.get("polish.command.back"), 2, Command.BACK);

        view = new CommCareListView(Localization.get("commcare.tools.title"));
        view.append(cUpdates);
        view.append(cNetwork);
        view.append(cPermissions);
        view.append(cMediaTest);
        view.addCommand(back);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.api.State#start()
     */
    public void start() {
        view.setCommandListener(this);
        J2MEDisplay.setView(view);
    }

    public void commandAction(Command c, Displayable d) {
        if(back.equals(c)) {
            done();
            return;
        } else {
            if(cUpdates == view.getCurrentItem()) {
                doUpdateCheck();
                return;
            } else if(cNetwork == view.getCurrentItem()) {
                doNetworkTest();
                return;
            } else if(cPermissions == view.getCurrentItem()) {
                doPermissionsTest();
                return;
            } else if(cMediaTest == view.getCurrentItem()) {
                doMediaValidation();
                return;
            }
        }
    }

    protected void doMediaValidation(){
        J2MEDisplay.startStateWithLoadingScreen(new CommCareValidationState("test message") {
            public void done() {
                CommCareToolsState.this.done();
            }
        });
    }

    protected void doUpdateCheck() {
        J2MEDisplay.startStateWithLoadingScreen(new CommCareUpgradeState(true) {
            public void done() {
                //Return to whatever called us (A little weird that the workflow is
                //different, but we want people who press the center button to
                //have a route back to the non-tools workflow here.
                CommCareToolsState.this.done();
            }
        });

    }

    protected void doNetworkTest() {
        new GPRSTestState () {
            public void done () {
                //Come back to the tools
                J2MEDisplay.startStateWithLoadingScreen(CommCareToolsState.this);
            }
        }.start();

    }

    protected void doPermissionsTest() {
        new PermissionsTestState () {
            public void done () {
                //Come back to the tools
                J2MEDisplay.startStateWithLoadingScreen(CommCareToolsState.this);
            }
        }.start();

    }
}
