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
		String results = CommCareStatic.validate(view);
		view.setMessage(results,false);
	}
	
	private void validationHelper(){
		view.setMessage(CommCareStartupInteraction.failSafeText("validation.start","Validating media..."));
		String validationResult = CommCareStatic.validate(view);
		
		if(validationResult == null){
			view.removeCommand(cmdRetry);
			view.setMessage(CommCareStartupInteraction.failSafeText("validation.success","Validation successful!"), false);
		} else{
			view.setMessage(validationResult, false);
		}
	}
	
	public String validate() {
		view.setMessage(CommCareStartupInteraction.failSafeText("install.verify","CommCare initialized. Validating multimedia files..."));
		SizeBoundUniqueVector<MissingMediaException> problems = new SizeBoundUniqueVector<MissingMediaException>(10);
		ResourceTable global = CommCareContext.RetrieveGlobalResourceTable();
		global.verifyInstallation(problems);
		if(problems.size() > 0 ) {
			int badImageRef = problems.getBadImageReferenceCount();
			int badAudioRef = problems.getBadAudioReferenceCount();
			int badVideoRef = problems.getBadVideoReferenceCount();
			String errorMessage	= "CommCare cannot start because you are missing multimedia files.";
			String message = CommCareStartupInteraction.failSafeText("install.bad",errorMessage, new String[] {""+badImageRef,""+badAudioRef,""+badVideoRef});
			Hashtable<String, Vector<String>> problemList = new Hashtable<String,Vector<String>>();
			for(Enumeration en = problems.elements() ; en.hasMoreElements() ;) {
				MissingMediaException ure = (MissingMediaException)en.nextElement();

				String res = ure.getResource().getResourceId();
				
				Vector<String> list;
				if(problemList.containsKey(res)) {
					list = problemList.get(res);
				} else{
					list = new Vector<String>();
				}
				
				// code to pretty up the output for mealz
				
				int substringIndex = ure.getMessage().indexOf("/commcare");
				
				String shortenedMessage = (ure.getMessage()).substring(substringIndex+1);
				
				list.addElement(shortenedMessage);
				
				problemList.put(res, list);

			}
			
			message += "\n-----------";
			
			for(Enumeration en = problemList.keys(); en.hasMoreElements();) {
				
				String resource = (String)en.nextElement();		
				//message += "\n-----------";
				for(String s : problemList.get(resource)) {
					message += "\n" + s;
				}
			}
			if(problems.getAdditional() > 0) {
				message += "\n\n..." + problems.getAdditional() + " more";
			}
			
			return message;
		}
		view.removeCommand(cmdRetry);
		return CommCareStartupInteraction.failSafeText("validation.success","Validation successful!");
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
