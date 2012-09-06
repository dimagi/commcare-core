/**
 * 
 */
package org.javarosa.engine;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Vector;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.AnswerDataFactory;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.engine.models.Action;
import org.javarosa.engine.models.ActionResponse;
import org.javarosa.engine.models.Command;
import org.javarosa.engine.models.Mockup;
import org.javarosa.engine.models.Session;
import org.javarosa.engine.models.Step;
import org.javarosa.engine.playback.BadPlaybackException;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.util.XFormUtils;

/**
 * @author ctsims
 *
 */
public class XFormPlayer {
	
	XFormEnvironment environment;
	FormEntryController fec;
	//FormIndex current;
	
	PrintStream out;
	InputStream in;
	
	BufferedReader reader;
	
	boolean forward = true;
	
	private Step current;
	
	Mockup mockup;
	
	public XFormPlayer(InputStream in, PrintStream out, Mockup mockup) {
		this.in = in;
		this.out = out;
		this.mockup = mockup;
	}
	
//	public XFormPlayer(Console console, PrintStream out) {
//		this.console = console;
//		this.out = out;
//	}
//	
	
	public void start(String formPath)  throws FileNotFoundException {
		this.start(XFormUtils.getFormFromInputStream(new FileInputStream(formPath)));
	}
	

	public void start(String formPath, Session session) throws FileNotFoundException {
		this.start(XFormUtils.getFormFromInputStream(new FileInputStream(formPath)), session);
	}
	
	
	public void start(FormDef form, Session session) {
		this.environment = new XFormEnvironment(form, session);
		fec = environment.setup();
		reader = new BufferedReader(new InputStreamReader(in));
		processLoop();
	}
	
	public void start(FormDef form) {
		this.environment = new XFormEnvironment(form, mockup);
		fec = environment.setup();
		reader = new BufferedReader(new InputStreamReader(in));
		processLoop();
	}	
	
	private static final int BLANKLINES = 10;
	
	private void clear() {
		String bl = "";
		for(int i = 0 ; i < BLANKLINES; ++i) {
			bl += "\r\n";
		}
		out.print(bl);
	}
	
	private void show(boolean forward) {
		clear();
		
		switch(fec.getModel().getEvent()) {
		case FormEntryController.EVENT_BEGINNING_OF_FORM:
			out.print("Form Start: Press Return to proceed");
			break;
		case FormEntryController.EVENT_END_OF_FORM:
			out.print("Form End: Press Return to Complete Entry");
			break;
		case FormEntryController.EVENT_GROUP:
			if(forward) {
				fec.stepToNextEvent();
			} else {
				fec.stepToPreviousEvent();
			}
			show(forward);
			break;
		case FormEntryController.EVENT_QUESTION:
			question();
			break;
		case FormEntryController.EVENT_REPEAT:
			if(forward) {
				fec.stepToNextEvent();
			} else {
				fec.stepToPreviousEvent();
			}
			show(forward);
			break;
		case FormEntryController.EVENT_REPEAT_JUNCTURE:
			out.print("Repeats Not Implemented, press return to exit");
			break;
		case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
			out.print("Repeats Not Implemented, press return to exit");
			break;
		}
	}
	
	/**
	 * Actually runs the app, performs blocking input, etc.
	 */
	private void processLoop() {
		boolean exit = false;
		try {
			while(!exit) {
				show(forward);
				forward = true;
				String input = blockForInput();
	
				//Command!
				if(input.startsWith(":")) {
					exit = command(input.substring(1));
				} else {
					//what we do depends on the current item
					exit = input(input);
				}
				//Commit current step
				environment.commitStep();
			}
		} catch(BadPlaybackException bpe) {
			bpe.printStackTrace();
			out.println("There was a problem with playing back the file! " + bpe.getMessage());
			return;
		}
		
		XFormSerializingVisitor visitor = new XFormSerializingVisitor();
		try {
			byte[] data = visitor.serializeInstance(fec.getModel().getForm().getInstance());
			clear();
			out.println(new String(data));
		} catch (IOException e) {
			out.println("Error Serializing XForm Data! " + e.getMessage());
		}
	}
	
	private boolean command(String command) throws BadPlaybackException {
		environment.recordAction(new Action(new Command(command)));
		if("next".equalsIgnoreCase(command)) {
			fec.stepToNextEvent();
			return false;
		} else if("back".equalsIgnoreCase(command)) {
			forward = false;
			fec.stepToPreviousEvent();
			return false;
		} else if("quit".equalsIgnoreCase(command)) {
			out.println("Quitting!");
			return true;
		} else if("finish".equalsIgnoreCase(command) && fec.getModel().getEvent() == FormEntryController.EVENT_END_OF_FORM) {
			out.println("Quitting!");
			return true;
		} else {
			badInput(command, "Invalid Command " + command);
			return false;
		}
	}
	
	private boolean input(String input) throws BadPlaybackException {
		switch(fec.getModel().getEvent()) {
		case FormEntryController.EVENT_BEGINNING_OF_FORM:
			environment.recordAction(new Action(new Command("next")));
			fec.stepToNextEvent();
			return false;
		case FormEntryController.EVENT_END_OF_FORM:
			environment.recordAction(new Action(new Command("finish")));
			return true;
		case FormEntryController.EVENT_QUESTION:
			FormEntryPrompt fep = fec.getModel().getQuestionPrompt();

			String actualInput = input;
			if(environment.isModePlayback()) {
				//for multiselects, etc.
				actualInput = this.current.getAction().getRawAnswer();
			} else {
				Vector<SelectChoice> choices = fep.getSelectChoices();
				if(choices != null) {
					try {
						int index = Integer.parseInt(input) - 1;
						if(index >= choices.size()) {
							badInput(input, "Enter a number between 1 and " + (choices.size()));
							return false;
						}
						actualInput = choices.elementAt(index).getValue();
					} catch(NumberFormatException nfe) {
						badInput(input, "Enter a number between 1 and " + (choices.size()));
						return false;
					}
				}
			}
			
			try {
				IAnswerData value = actualInput.equals("") ? null : AnswerDataFactory.template(fep.getControlType(), fep.getDataType()).cast(new UncastData(actualInput));
				int response = fec.answerQuestion(value);
				
				if(environment.isModePlayback()) {
					ActionResponse actionResponse = current.getAction().getActionResponse();
					actionResponse.validate(response, actualInput, fep);
				}
				
				
				if(response == FormEntryController.ANSWER_OK) {
					environment.recordAction(new Action(actualInput));
					fec.stepToNextEvent();
					return false;
				} else if(response == FormEntryController.ANSWER_REQUIRED_BUT_EMPTY) {
					environment.recordAction(new Action(actualInput, ActionResponse.QuestionRequired()));
					badInput(input,"Answer Is Required!");
					return false;

				} else if(response == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
					environment.recordAction(new Action(actualInput, ActionResponse.ConstraintViolated()));
					badInput(input, fep.getConstraintText());
					return false;
				}
			} catch(Exception e) {
				e.printStackTrace();
				badInput(input, e.getMessage());
				return false;
			}
			return false;
		case FormEntryController.EVENT_REPEAT:
			return true;
		case FormEntryController.EVENT_REPEAT_JUNCTURE:
			return true;
		case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
			return true;
		}
		out.println("Bad state! Quitting...");
		return true;
	}
	
	private void badInput(String input) throws BadPlaybackException {
		badInput(input, null);
	}
	
	private void badInput(String input, String msg) throws BadPlaybackException {		
		String message = "Input " + input + " is invalid!";
		if(msg != null) {
			message += " " + msg;
		}		
		
		if(environment.isModePlayback()) {
			throw new BadPlaybackException("Invalid input during playback: " +message);
		}
		
		out.println(message);
		out.println("Press Return to Try Again");
		blockForInput();
	}

	private String blockForInput() {
		try {
			if(environment.isModePlayback()) {
				this.current = environment.popStep();
				return current.getAction().getInputString();
			}
			return reader.readLine().trim();
		} catch (IOException e) {
			out.println("Bad input! Gotta quit...");
			System.exit(-1);
			return null;
		}
	}

	private void question() {
		FormEntryPrompt fep = fec.getModel().getQuestionPrompt();
		String text = fep.getQuestionText();
		out.println(text);
		
		Vector<SelectChoice> choices = fep.getSelectChoices();
		if(choices != null) {
			for(int i = 0 ; i < choices.size() ; ++i) {
				System.out.println((i+1) + ") " + fep.getSelectChoiceText(choices.elementAt(i)));
			}
		}
		
		if(fep.getControlType() == Constants.CONTROL_TRIGGER) {
			System.out.println("Press Return to Proceed");
		}
	}
}