package org.javarosa.form.api;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.AnswerDataFactory;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.TreeReference;

/**
 * Replay form entry session. Steps through form, applying answers from the
 * session to corresponding questions by matching form indices. Replay aborts
 * if form indices don't match and stops when the session is empty.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class FormEntrySessionReplayer {
    private final FormEntrySession formEntrySession;
    private final FormEntryController formEntryController;

    private FormEntrySessionReplayer(FormEntryController formEntryController,
                                     FormEntrySession formEntrySession) {
        this.formEntryController = formEntryController;
        this.formEntrySession = formEntrySession;
    }

    public static void tryReplayingFormEntry(FormEntryController formEntryController,
                                             FormEntrySession formEntrySession) {
        FormEntrySessionReplayer replayer =
                new FormEntrySessionReplayer(formEntryController, formEntrySession);
        if (replayer.hasSessionToReplay()) {
            replayer.replayForm();
        }
    }

    private boolean hasSessionToReplay() {
        return formEntrySession != null && formEntrySession.size() > 0;
    }

    /**
     * TODO AMS: If the question corresponding to the stopping ref has been removed, this will
     * never return true and replay will take the user all the way to the end of the form
     */
    private boolean reachedEndOfReplay(String lastQuestionRefReplayed) {
        return lastQuestionRefReplayed.equals(formEntrySession.getStopRef());
    }

    private void replayForm() {
        formEntryController.jumpToIndex(FormIndex.createBeginningOfFormIndex());
        int event = formEntryController.stepToNextEvent(FormEntryController.STEP_INTO_GROUP);
        String lastQuestionRefReplayed = "";
        while (event != FormEntryController.EVENT_END_OF_FORM && hasSessionToReplay()
                && !reachedEndOfReplay(lastQuestionRefReplayed)) {
            lastQuestionRefReplayed = replayEvent(event);
            event = formEntryController.stepToNextEvent(FormEntryController.STEP_INTO_GROUP);
        }
        formEntryController.stepToPreviousEvent();
    }

    private String replayEvent(int event) {
        if (event == FormEntryController.EVENT_QUESTION) {
            return replayQuestion();
        } else if (event == FormEntryController.EVENT_PROMPT_NEW_REPEAT) {
            return checkForRepeatCreation();
            // TODO PLM: can't handle proceeding to end of form after "Don't add" action
        }
        return "";
    }

    private String checkForRepeatCreation() {
        TreeReference questionRef = formEntryController.getModel().getFormIndex().getReference();
        if (formEntrySession.getAndRemoveRepeatActionForRef(questionRef)) {
            formEntryController.newRepeat();
        }
        return questionRef.toString();
    }

    private String replayQuestion() {
        FormIndex questionIndex = formEntryController.getModel().getFormIndex();
        TreeReference questionRef = questionIndex.getReference();
        FormEntryAction action = formEntrySession.getAndRemoveActionForRef(questionRef);
        if (action != null) {
            if (!action.isSkipAction()) {
                FormEntryPrompt entryPrompt =
                        formEntryController.getModel().getQuestionPrompt(questionIndex);
                IAnswerData answerData =
                        AnswerDataFactory.template(entryPrompt.getControlType(),
                                entryPrompt.getDataType()).cast(new UncastData(action.getValue()));
                formEntryController.answerQuestion(questionIndex, answerData);
            }
        }
        return questionRef.toString();
    }

    public static class ReplayError extends RuntimeException {
        public ReplayError(String msg) {
            super(msg);
        }
    }
}
