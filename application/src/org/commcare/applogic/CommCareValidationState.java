package org.commcare.applogic;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCareContext;
import org.commcare.view.CommCareStartupInteraction;
import org.javarosa.core.api.State;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.SizeBoundVector;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
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
		String results = validate();
		view.setMessage(results,false);
	}
	
	private void validationHelper(){
		view.setMessage(CommCareStartupInteraction.failSafeText("validation.start","Validating media..."));
		view.setMessage(validate(), false);
	}
	
	private String validate() {
		view.setMessage(CommCareStartupInteraction.failSafeText("install.verify","CommCare initialized. Validating multimedia files..."));
		SizeBoundVector<UnresolvedResourceException> problems = new SizeBoundVector<UnresolvedResourceException>(10);
		ResourceTable global = CommCareContext.RetrieveGlobalResourceTable();
		global.verifyInstallation(problems);
		if(problems.size() > 0 ) {
			int badImageRef = problems.getBadImageReferenceCount();
			int badAudioRef = problems.getBadAudioReferenceCount();
			int badVideoRef = problems.getBadVideoReferenceCount();
			String errorMessage = "CommCare cannot start because you are missing multimedia files.";
			String message = CommCareStartupInteraction.failSafeText("install.bad",errorMessage, new String[] {""+badImageRef,""+badAudioRef,""+badVideoRef});
			Hashtable<String, Vector<String>> problemList = new Hashtable<String,Vector<String>>();
			for(Enumeration en = problems.elements() ; en.hasMoreElements() ;) {
				UnresolvedResourceException ure = (UnresolvedResourceException)en.nextElement();

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
