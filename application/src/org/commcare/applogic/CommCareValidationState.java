package org.commcare.applogic;

/**
 * State class that handles validation, interaction, and message output. Only called by
 * the CommCareToolsState - initial validation is handled in the CommCareContext class.
 *
 * @author wspride
 */

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.resources.model.MissingMediaException;
import org.commcare.resources.model.ResourceTable;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareStatic;
import org.commcare.view.CommCareStartupInteraction;
import org.javarosa.core.api.State;
import org.javarosa.core.util.SizeBoundUniqueVector;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.j2me.view.J2MEDisplay;

import de.enough.polish.ui.Command;
import de.enough.polish.ui.CommandListener;
import de.enough.polish.ui.Displayable;

public abstract class CommCareValidationState implements State, CommandListener, TrivialTransitions{

    CommCareStartupInteraction view;
    public Command cmdRetry = new Command("Retry", Command.OK, 1);
    public Command cmdExit = new Command("Exit", Command.CANCEL, 1);

    public CommCareValidationState(String msg){
        view = new CommCareStartupInteraction(msg);
        view.addCommand(cmdRetry);
        view.addCommand(cmdExit);
        view.setCommandListener(this);
    }

    public void start() {
        // TODO Auto-generated method stub
        view.setCommandListener(this);
        view.setMessage(CommCareStartupInteraction.failSafeText("validation.start","Validating media..."));
        J2MEDisplay.setView(view);
        String results = CommCareStatic.validate(CommCareContext.RetrieveGlobalResourceTable());
        view.setMessage(results,false);
    }

    private void validationHelper(){
        view.setMessage(CommCareStartupInteraction.failSafeText("validation.start","Validating media..."));
        String validationResult = CommCareStatic.validate(CommCareContext.RetrieveGlobalResourceTable());

        if(validationResult == null){
            view.removeCommand(cmdRetry);
            view.setMessage(CommCareStartupInteraction.failSafeText("validation.success","Validation successful!"), false);
        } else{
            view.setMessage(validationResult, false);
        }
    }

    public void commandAction(Command c, Displayable d) {
        if(c==cmdRetry){
            validationHelper();
        }
        if(c==cmdExit){
            done();
        }
    }

    public abstract void done();

}
