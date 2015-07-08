package org.commcare.applogic;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import org.javarosa.core.api.State;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;

public abstract class CommCareAlertState implements State, HandledCommandListener {

    private String title;
    private String content;

    public CommCareAlertState (String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void start() {
        J2MEDisplay.showError(title, content, null, null, this);
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command c, Displayable d) {
        done();
    }

    public abstract void done ();
}