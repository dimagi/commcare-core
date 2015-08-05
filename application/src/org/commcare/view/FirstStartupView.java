/**
 *
 */
package org.commcare.view;

import org.commcare.api.transitions.FirstStartupTransitions;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import de.enough.polish.ui.Form;
import de.enough.polish.ui.StringItem;

/**
 * @author ctsims
 *
 */
public class FirstStartupView extends Form implements HandledCommandListener {

    private static final Command RESTORE = new Command(Localization.get("intro.restore"), Command.ITEM, 1);
    private static final Command LOGIN = new Command(Localization.get("intro.start"),Command.ITEM, 1);
    private static final Command EXIT = new Command(Localization.get("polish.command.exit"),Command.EXIT, 1);

    StringItem intro;

    StringItem restore;
    StringItem login;

    FirstStartupTransitions transitions;

    public FirstStartupView(FirstStartupTransitions transitions) {
        super(Localization.get("intro.title"));
        intro = new StringItem("", Localization.get("intro.text"));
        this.append(intro);

        //#style button
        restore = new StringItem("", Localization.get("intro.restore"));
        restore.setDefaultCommand(RESTORE);

        //#style button
        login = new StringItem("", Localization.get("intro.start"));
        login.setDefaultCommand(LOGIN);

        this.append(login);
        this.append(restore);
        this.transitions = transitions;
        this.setCommandListener(this);
    }

    public void _commandAction(Command c, Displayable d) {
        if(c == RESTORE) {
            transitions.restore();
        } else if(c == LOGIN) {
            transitions.login();
        } else if(c == EXIT) {
            transitions.exit();
        }
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this,c,d);
    }
}
