package org.javarosa.engine;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.AnswerDataFactory;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.trace.StringEvaluationTraceSerializer;
import org.javarosa.engine.models.Action;
import org.javarosa.engine.models.ActionResponse;
import org.javarosa.engine.models.Command;
import org.javarosa.engine.models.Mockup;
import org.javarosa.engine.models.Session;
import org.javarosa.engine.models.Step;
import org.javarosa.engine.playback.BadPlaybackException;
import org.javarosa.engine.xml.XmlUtil;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.form.api.FormEntrySession;
import org.javarosa.form.api.FormEntrySessionReplayer;
import org.javarosa.model.xform.DataModelSerializer;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.util.XFormUtils;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Vector;

/**
 * @author ctsims
 */
public class XFormPlayer {

    XFormEnvironment environment;
    private FormEntryController fec;
    private InstanceInitializationFactory mIIF;
    //FormIndex current;

    private final PrintStream out;

    public enum FormResult {
        Cancelled,
        Entry_Error,
        Output_Error,
        Quit,
        Unknown,
        Completed
    }

    private boolean mProcessOnExit = false;
    private FormResult mExecutionResult = FormResult.Unknown;

    private byte[] mExecutionInstance;

    private BufferedReader reader;

    private boolean forward = true;

    private Step current;

    private boolean[] mCurrentSelectList;
    private FormIndex mCurrentSelectIndex;

    private boolean mInEvalMode = false;
    private boolean mIsDebugOn = false;

    private String mPreferredLocale;

    private final Mockup mockup;

    public XFormPlayer(InputStream in, PrintStream out, Mockup mockup) {
        this.reader = new BufferedReader(new InputStreamReader(in));
        this.out = out;
        this.mockup = mockup;
    }

    public XFormPlayer(BufferedReader in, PrintStream out, Mockup mockup) {
        this.reader = in;
        this.out = out;
        this.mockup = mockup;
    }

    public void setPreferredLocale(String locale) {
        this.mPreferredLocale = locale;
    }

    public void start(String formPath) throws FileNotFoundException {
        this.start(XFormUtils.getFormFromInputStream(new FileInputStream(formPath)));
    }

    public void start(FormDef form) {
        this.environment = new XFormEnvironment(form, mockup);
        if (mPreferredLocale != null) {
            this.environment.setLocale(mPreferredLocale);
        }
        fec = environment.setup(this.mIIF);
        processLoop();
    }

    private static final int BLANKLINES = 10;

    private void clear() {
        String bl = "";
        for (int i = 0; i < BLANKLINES; ++i) {
            bl += "\r\n";
        }
        out.print(bl);
    }

    private void show(boolean forward) {
        clear();

        switch (fec.getModel().getEvent()) {
            case FormEntryController.EVENT_BEGINNING_OF_FORM:
                out.print("Form Start: Press Return to proceed");
                break;
            case FormEntryController.EVENT_END_OF_FORM:
                out.print("Form End: Press Return to Complete Entry");
                mProcessOnExit = true;
                break;
            case FormEntryController.EVENT_GROUP:
                if (forward) {
                    fec.stepToNextEvent();
                } else {
                    fec.stepToPreviousEvent();
                }
                show(forward);
                break;
            case FormEntryController.EVENT_QUESTION:
                question(fec.getModel().getQuestionPrompt());
                break;
            case FormEntryController.EVENT_REPEAT:
                if (forward) {
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
                newRepeatQuestion();
                break;
        }
    }

    private void newRepeatQuestion() {
        out.println("Add new repeat?");
        out.println("1) Yes, add a new repeat group");
        out.println("2) No, continue to the next question");
    }

    /**
     * Actually runs the app, performs blocking input, etc.
     */
    private void processLoop() {
        boolean exit = false;
        try {
            while (!exit) {
                if (!mInEvalMode) {
                    show(forward);
                }
                forward = true;
                String input = blockForInput();

                if (mInEvalMode) {
                    //If we're in evalMode we wanna process all input in order from
                    //this point forth until we exit eval mode
                    evalModeInput(input);

                    //Don't wanna add any eval mode stuff to the execution history.
                    continue;
                }
                try {
                    //Command!
                    if (input.startsWith(":")) {
                        exit = command(input.substring(1));
                    } else {
                        //what we do depends on the current item
                        exit = answerQuestion(input);
                    }
                } catch (InvalidInputException e) {
                    //User will retry after receiving message
                    exit = false;
                }
                //Commit current step
                environment.commitStep();
            }
        } catch (BadPlaybackException bpe) {
            bpe.printStackTrace();
            out.println("There was a problem with playing back the file! " + bpe.getMessage());
            return;
        }
        //Form is finished.
        //TODO: Final constraint/etc validation
        if (mProcessOnExit) {
            fec.getModel().getForm().postProcessInstance();
            serializeResult();
        }
    }

    private void serializeResult() {
        XFormSerializingVisitor visitor = new XFormSerializingVisitor();
        try {
            mExecutionInstance = visitor.serializeInstance(fec.getModel().getForm().getInstance());
            clear();
            out.println(new String(mExecutionInstance));
            mExecutionResult = FormResult.Completed;
        } catch (IOException e) {
            out.println("Error Serializing XForm Data! " + e.getMessage());
            mExecutionResult = FormResult.Output_Error;
        }
    }

    public FormResult getExecutionResult() {
        return mExecutionResult;
    }

    public InputStream getResultStream() {
        return new ByteArrayInputStream(mExecutionInstance);
    }

    /**
     * Evaluate input to eval mode, and exit eval mode if
     * the input is blank.
     */
    private void evalModeInput(String evalModeInput) {
        if (evalModeInput.equals("")) {
            this.mInEvalMode = false;
            out.println("exiting eval mode");
        } else {
            evalExpression(evalModeInput);
        }
    }

    private boolean command(String command) throws BadPlaybackException, InvalidInputException {
        environment.recordAction(new Action(new Command(command)));
        if ("next".equalsIgnoreCase(command)) {
            fec.stepToNextEvent();
            return false;
        } else if ("back".equalsIgnoreCase(command)) {
            forward = false;
            fec.stepToPreviousEvent();
            return false;
        } else if ("quit".equalsIgnoreCase(command)) {
            out.println("Quitting!");
            return true;
        } else if ("cancel".equalsIgnoreCase(command)) {
            out.println("Cancelling form entry!");
            mExecutionResult = FormResult.Cancelled;
            return true;
        } else if ("finish".equalsIgnoreCase(command)) {
            out.println("Attempting to finish the form in its current state...");
            mProcessOnExit = true;
            return true;
        } else if (command.startsWith("print")) {
            int spaceIndex = command.indexOf(" ");
            if (command.length() == spaceIndex || spaceIndex == -1) {
                printInstance(out, fec.getModel().getForm().getInstance());
            } else {
                //This is the instance the user wants to print
                String arg = command.substring(spaceIndex + 1);
                printExternalInstance(out, arg);
            }
            return false;
        } else if (command.startsWith("eval")) {
            int spaceIndex = command.indexOf(" ");
            if (command.length() == spaceIndex || spaceIndex == -1) {
                out.println("Entering eval mode, exit by entering a blank line");
                this.mInEvalMode = true;
                return false;
            }
            String arg = command.substring(spaceIndex + 1);
            evalExpression(arg);
            return false;
        } else if (command.startsWith("replay")) {
            command = command.trim();
            int spaceIndex = command.indexOf(" ");
            if (spaceIndex != -1) {
                String sessionString = command.substring(spaceIndex + 1);
                try {
                    FormEntrySessionReplayer.tryReplayingFormEntry(fec, FormEntrySession.fromString(sessionString));
                } catch (FormEntrySessionReplayer.ReplayError e) {
                    out.println("Error replaying form: " + e.getMessage());
                    out.println("Aborting form entry");
                    return true;
                }
            } else {
                out.println("Invalid command, please provide session string to replay");
            }
            return false;
        } else if (command.startsWith("entry-session")) {
            out.println(fec.getFormEntrySessionString());
            return false;
        } else if (command.startsWith("relevant")) {
            displayRelevant();
            return false;
        } else if (command.startsWith("debug")) {
            mIsDebugOn = !mIsDebugOn;
            out.println("Expression Debugging: " + (mIsDebugOn ? "ENABLED" : "DISABLED"));
            return false;
        } else {
            badInput(command, "Invalid Command " + command);
            return false;
        }
    }

    private void displayRelevant() {
        FormIndex current = this.fec.getModel().getFormIndex();
        String output = this.fec.getModel().getDebugInfo(current, "relevant", new StringEvaluationTraceSerializer());
        if (output == null) {
            out.println("No display logic defined");
        } else {
            out.println(output);
        }
    }

    private void evalExpression(String xpath) {
        out.println(xpath);
        XPathExpression expr;
        try {
            expr = XPathParseTool.parseXPath(xpath);
        } catch (XPathSyntaxException e) {
            out.println("Error (parse): " + e.getMessage());
            return;
        }
        EvaluationContext ec = fec.getModel().getForm().getEvaluationContext();

        //See if we're on a valid index, if so use that as our EC base
        FormIndex current = this.fec.getModel().getFormIndex();
        if (current.isInForm()) {
            ec = new EvaluationContext(ec, current.getReference());
        }

        if (mIsDebugOn) {
            ec.setDebugModeOn();
        }

        try {
            Object val = expr.eval(ec);
            out.println(getDisplayString(val));
        } catch (Exception e) {
            out.println("Error  (eval): " + e.getMessage());
            return;
        }

        if (mIsDebugOn && ec.getEvaluationTrace() != null) {
            out.println(new StringEvaluationTraceSerializer().serializeEvaluationLevels(ec.getEvaluationTrace()));
        }
    }

    private static String getDisplayString(Object value) {
        if (value instanceof XPathNodeset) {
            return FunctionUtils.getSerializedNodeset((XPathNodeset)value);
        } else {
            return FunctionUtils.toString(value);
        }
    }

    private void printExternalInstance(PrintStream out, String instanceRef) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataModelSerializer s = new DataModelSerializer(bos, mIIF);

            s.serialize(new ExternalDataInstance(instanceRef, "instance"), null);

            out.println(XmlUtil.getPrettyXml(bos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
            out.println("Error Serializing XForm Data! " + e.getMessage());
        }
    }

    private static void printInstance(PrintStream out, FormInstance instance) {
        XFormSerializingVisitor visitor = new XFormSerializingVisitor();
        try {
            byte[] data = visitor.serializeInstance(instance);
            out.println(XmlUtil.getPrettyXml(data));
        } catch (IOException e) {
            e.printStackTrace();
            out.println("Error Serializing XForm Data! " + e.getMessage());
        }
    }

    private boolean answerQuestion(String input) throws BadPlaybackException, InvalidInputException {
        switch (fec.getModel().getEvent()) {
            case FormEntryController.EVENT_BEGINNING_OF_FORM:
                environment.recordAction(new Action(new Command("next")));
                fec.stepToNextEvent();
                return false;
            case FormEntryController.EVENT_END_OF_FORM:
                environment.recordAction(new Action(new Command("finish")));
                mProcessOnExit = true;
                return true;
            case FormEntryController.EVENT_QUESTION:
                FormEntryPrompt fep = fec.getModel().getQuestionPrompt();

                String actualInput = input;
                if (environment.isModePlayback()) {
                    //for multiselects, etc.
                    actualInput = this.current.getAction().getRawAnswer();
                } else {
                    Vector<SelectChoice> choices = fep.getSelectChoices();
                    if (choices != null) {
                        if (input.equals("") &&
                                fep.getQuestion().getControlType() == Constants.CONTROL_SELECT_MULTI) {
                            Vector<Selection> answers = new Vector<>();
                            for (int i = 0; i < mCurrentSelectList.length; ++i) {
                                if (mCurrentSelectList[i]) {
                                    answers.addElement(choices.elementAt(i).selection());
                                }
                            }
                            actualInput = new SelectMultiData(answers).uncast().getString();
                        } else {
                            int index = parseAndValidate(input, choices.size()) - 1;

                            if (fep.getQuestion().getControlType() == Constants.CONTROL_SELECT_ONE) {
                                actualInput = choices.elementAt(index).getValue();
                            }

                            if (fep.getQuestion().getControlType() == Constants.CONTROL_SELECT_MULTI) {
                                this.mCurrentSelectList[index] = !this.mCurrentSelectList[index];
                                return false;
                            }
                        }
                    }
                }

                try {
                    IAnswerData value = actualInput.equals("") ? null : AnswerDataFactory.template(fep.getControlType(), fep.getDataType()).cast(new UncastData(actualInput));
                    int response = fec.answerQuestion(value);

                    if (environment.isModePlayback()) {
                        ActionResponse actionResponse = current.getAction().getActionResponse();
                        actionResponse.validate(response, actualInput, fep);
                    }

                    if (response == FormEntryController.ANSWER_OK) {
                        environment.recordAction(new Action(actualInput));
                        fec.stepToNextEvent();
                        return false;
                    } else if (response == FormEntryController.ANSWER_REQUIRED_BUT_EMPTY) {
                        environment.recordAction(new Action(actualInput, ActionResponse.QuestionRequired()));
                        badInput(input, "Answer Is Required!");
                        return false;

                    } else if (response == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
                        environment.recordAction(new Action(actualInput, ActionResponse.ConstraintViolated()));
                        badInput(input, fep.getConstraintText());
                        return false;
                    }
                } catch(InvalidInputException e) {
                    //This is handled by the outer loop processor, so make sure we don't
                    //absorb it below
                    throw e;
                } catch (Exception e) {
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
                int index = parseAndValidate(input, 2);
                if (index == 1) {
                    fec.newRepeat();
                    fec.stepToNextEvent();
                    return false;
                } else if (index == 2) {
                    fec.stepToNextEvent();
                    return false;
                }
        }
        out.println("Bad state! Quitting...");
        return true;
    }

    private int parseAndValidate(String input, int max) throws BadPlaybackException, InvalidInputException {
        int i;
        try {
            i = Integer.parseInt(input);
            if (i < 1 || i > max) {
                badInput(input, "Enter a number between 1 and " + max);
                throw new InvalidInputException();
            }
        } catch (NumberFormatException nfe) {
            badInput(input, "Enter a number between 1 and " + max);
            throw new InvalidInputException();
        }
        return i;
    }

    private void badInput(String input, String msg) throws BadPlaybackException, InvalidInputException {
        String message = "Input " + input + " is invalid!";
        if (msg != null) {
            message += " " + msg;
        }

        if (environment.isModePlayback()) {
            throw new BadPlaybackException("Invalid input during playback: " + message);
        }

        out.println(message);
        out.println("Press Return to Try Again");
        blockForInput();
        throw new InvalidInputException();
    }

    private String blockForInput() {
        try {
            if (environment.isModePlayback()) {
                this.current = environment.popStep();
                return current.getAction().getInputString();
            }
            return reader.readLine().trim();
        } catch (IOException e) {
            out.println("Bad input! Gotta quit...");
            throw new RuntimeException(e);
        }
    }

    private void question(FormEntryPrompt fep) {
        String text = fep.getQuestionText();
        out.println(text);

        Vector<SelectChoice> choices = fep.getSelectChoices();
        if (choices != null) {
            initSelectList(fep);
            for (int i = 0; i < choices.size(); ++i) {
                String prefix = "";
                if (fep.getControlType() == Constants.CONTROL_SELECT_MULTI) {
                    prefix = "[" + (mCurrentSelectList[i] ? "X" : " ") + "] ";
                }
                System.out.println(prefix + (i + 1) + ") " + fep.getSelectChoiceText(choices.elementAt(i)));
            }
        }

        if (fep.getControlType() == Constants.CONTROL_TRIGGER) {
            System.out.println("Press Return to Proceed");
        }
    }

    private void initSelectList(FormEntryPrompt fep) {
        if (fep.getControlType() != Constants.CONTROL_SELECT_MULTI) {
            return;
        }

        if (!fep.getIndex().equals(mCurrentSelectIndex)) {
            mCurrentSelectIndex = null;
        }

        if (mCurrentSelectIndex != null) {
            return;
        }

        mCurrentSelectIndex = fep.getIndex();

        Vector<SelectChoice> choices = fep.getSelectChoices();
        mCurrentSelectList = new boolean[choices.size()];

        IAnswerData data = fep.getAnswerValue();
        if (data == null) {
            //default is false
            return;
        }
        SelectMultiData selectData = new SelectMultiData().cast(data.uncast());

        for (int i = 0; i < choices.size(); ++i) {
            if (selectData.isInSelection(choices.elementAt(i).getValue())) {
                mCurrentSelectList[i] = true;
            }
        }
    }

    public void setSessionIIF(InstanceInitializationFactory iif) {
        mIIF = iif;
    }

    private static final class InvalidInputException extends Exception {

    }
}
