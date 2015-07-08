package org.commcare.applogic;

import org.javarosa.core.api.State;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.core.util.TrivialTransitionsWithErrors;

public abstract class SendAllUnsentState implements TrivialTransitions, TrivialTransitionsWithErrors, State {

    public void start () {
        SendAllUnsentController controller = getController();
        controller.setTransitions(this);
        controller.start();
    }

    protected SendAllUnsentController getController () {
        return new SendAllUnsentController();
    }

    public void done (boolean errorsOccurred) {
        done();
    }

    //Re-declaration for S40 Bug workaround
    public abstract void done();
}
