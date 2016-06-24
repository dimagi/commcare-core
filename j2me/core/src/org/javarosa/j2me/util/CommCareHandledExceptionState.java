/**
 *
 */
package org.javarosa.j2me.util;

import org.javarosa.core.api.State;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.j2me.view.J2MEDisplay;

import de.enough.polish.ui.Command;
import de.enough.polish.ui.CommandListener;
import de.enough.polish.ui.Displayable;
import de.enough.polish.ui.TextBox;
import de.enough.polish.ui.TextField;

/**
 * @author wspride
 *
 */
public abstract class CommCareHandledExceptionState implements State, CommandListener, TrivialTransitions {

    TextBox view;
    Command back;
    Exception handledException;
    String errorMessage;

    public CommCareHandledExceptionState() {

        back = new Command(Localization.get("polish.command.back"), 2, Command.BACK);
        view = new TextBox("Runtime Error","",25,TextField.ANY);
        view.setConstraints(TextField.UNEDITABLE);
        view.addCommand(back);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.api.State#start()
     */
    public void start() {
        view.setCommandListener(this);
        J2MEDisplay.setView(view);
        setDisplayText();
    }

    public void commandAction(Command c, Displayable d) {
        if(back.equals(c)) {
            done();
            return;
        } else {}
    }

    public String getErrorMessage(){
        return errorMessage;
    }

    public void setErrorMessage(String errMessage){
        errorMessage = errMessage;
        setDisplayText();
    }

    public abstract String getExplanationMessage(String e);


    private void setDisplayText(){
        view.setString(getExplanationMessage(errorMessage));
    }

    public abstract boolean handlesException(Exception e);

    public abstract void done();

}
