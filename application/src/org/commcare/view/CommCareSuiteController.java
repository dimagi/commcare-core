/**
 *
 */
package org.commcare.view;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import org.commcare.api.transitions.MenuTransitions;
import org.commcare.suite.model.Menu;
import org.commcare.util.CommCareSessionController;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;

/**
 * @author ctsims
 *
 */
public class CommCareSuiteController implements HandledCommandListener {

    public final static Command BACK = new Command(Localization.get("command.back"), Command.BACK, 0);


    CommCareListView view;
    MenuTransitions transitions;
    CommCareSessionController controller;

    Menu m;
    public CommCareSuiteController(CommCareSessionController controller, Menu m) {
        this.m = m;
        this.controller = controller;

        //Strip any hanging menu stuff from this title
        view = new CommCareListView(Localizer.processArguments(m.getName().evaluate(), new String[]{""}));
        view.setCommandListener(this);
        view.addCommand(BACK);
    }

    public void setTransitions (MenuTransitions transitions) {
        this.transitions = transitions;
    }

    public void start() {
        view.deleteAll();
        controller.populateMenu(view, m.getId(), view);
        J2MEDisplay.setView(view);
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command c, Displayable d) {
        if(c.equals(List.SELECT_COMMAND)) {
            controller.chooseSessionItem(view.getSelectedIndex());
            controller.next();
        }
        else if(c.equals(BACK)) {
            transitions.exitMenuTransition();
        }
    }
}
