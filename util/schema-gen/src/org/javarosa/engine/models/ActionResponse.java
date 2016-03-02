package org.javarosa.engine.models;

import org.javarosa.engine.playback.BadPlaybackException;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

/**
 * @author ctsims
 */
public class ActionResponse {
    final int code;

    private ActionResponse(int code) {
        this.code = code;
    }

    public static ActionResponse ConstraintViolated() {
        return new ActionResponse(FormEntryController.ANSWER_CONSTRAINT_VIOLATED);
    }

    public static ActionResponse QuestionRequired() {
        return new ActionResponse(FormEntryController.ANSWER_REQUIRED_BUT_EMPTY);
    }

    public static ActionResponse AnswerOk() {
        return new ActionResponse(FormEntryController.ANSWER_OK);
    }

    public void validate(int response, String input, FormEntryPrompt fep) throws BadPlaybackException {
        String message = "Problem with question "+ fep.getQuestionText() + ": ";
        if(response != this.code) {
            if(code == FormEntryController.ANSWER_OK) {
                if(response == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
                    message += "Input '" +  input + "' was previously valid, but now violates a constraint";
                } else if (code == FormEntryController.ANSWER_REQUIRED_BUT_EMPTY) {
                    message += " This question is now required, but was skipped before.";
                }


                throw new BadPlaybackException(message);

            } else if(code == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
                message += " the input '" + input + "' used to violate a constraint";

                throw new BadPlaybackException(message);

            } else if(code == FormEntryController.ANSWER_REQUIRED_BUT_EMPTY) {

                message += " this question used to be required, and is now not required";

                throw new BadPlaybackException(message);
            }
        }
    }
}
