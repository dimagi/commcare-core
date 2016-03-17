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
        if (replayer.isRestoringFormSession()) {
            replayer.replayForm();
        }
    }

    private boolean isRestoringFormSession() {
        return formEntrySession != null && formEntrySession.size() > 0;
    }

    private void replayForm() {
        formEntryController.jumpToIndex(FormIndex.createBeginningOfFormIndex());
        int event = formEntryController.stepToNextEvent(FormEntryController.STEP_INTO_GROUP);
        while (event != FormEntryController.EVENT_END_OF_FORM && isRestoringFormSession()) {
            replayEvent(event);
            event = formEntryController.stepToNextEvent(FormEntryController.STEP_INTO_GROUP);
        }
    }

    private void replayEvent(int event) {
        if (event == FormEntryController.EVENT_QUESTION) {
            replayQuestion();
        } else if (event == FormEntryController.EVENT_PROMPT_NEW_REPEAT) {
            checkForRepeatCreation();
            // TODO PLM: can't handle proceeding to end of form after "Don't add" action
        }
    }

    private void checkForRepeatCreation() {
        TreeReference questionRef = formEntryController.getModel().getFormIndex().getReference();
        if (formEntrySession.getAndRemoveRepeatActionForRef(questionRef)) {
            formEntryController.newRepeat();
        }
    }

    private void replayQuestion() {
        FormIndex questionIndex = formEntryController.getModel().getFormIndex();
        TreeReference questionRef = questionIndex.getReference();
        FormEntryAction action = formEntrySession.getActionForRef(questionRef);
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
    }

    public static class ReplayError extends RuntimeException {
        public ReplayError(String msg) {
            super(msg);
        }
    }
}
