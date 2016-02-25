package org.javarosa.form.api;

import org.javarosa.core.model.actions.Action;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.InvalidReferenceException;
import org.javarosa.core.model.instance.TreeElement;

/**
 * This class is used to navigate through an xform and appropriately manipulate
 * the FormEntryModel's state.
 */
public class FormEntryController {
    public static final int ANSWER_OK = 0;
    public static final int ANSWER_REQUIRED_BUT_EMPTY = 1;
    public static final int ANSWER_CONSTRAINT_VIOLATED = 2;

    public static final int EVENT_BEGINNING_OF_FORM = 0;
    public static final int EVENT_END_OF_FORM = 1;
    public static final int EVENT_PROMPT_NEW_REPEAT = 2;
    public static final int EVENT_QUESTION = 4;
    public static final int EVENT_GROUP = 8;
    public static final int EVENT_REPEAT = 16;
    public static final int EVENT_REPEAT_JUNCTURE = 32;

    private final FormEntryModel model;
    private final FormEntrySessionRecorder formEntrySession;

    public static final boolean STEP_OVER_GROUP = true;
    public static final boolean STEP_INTO_GROUP = false;

    /**
     * Creates a new form entry controller for the model provided
     */
    public FormEntryController(FormEntryModel model) {
        this(model, new DummyFormEntrySession());
    }

    private FormEntryController(FormEntryModel model, FormEntrySessionRecorder formEntrySession) {
        this.model = model;
        this.formEntrySession = formEntrySession;
    }

    /**
     * Builds controller that records form entry actions to human readable
     * format that allows for replaying
     */
    public static FormEntryController buildRecordingController(FormEntryModel model) {
        return new FormEntryController(model, new FormEntrySession());
    }

    public FormEntryModel getModel() {
        return model;
    }

    /**
     * Attempts to save answer at the current FormIndex into the datamodel.
     */
    public int answerQuestion(IAnswerData data) {
        return answerQuestion(model.getFormIndex(), data);
    }

    /**
     * Attempts to save the answer at the specified FormIndex into the
     * datamodel.
     *
     * @return OK if save was successful, error if a constraint was violated.
     */
    public int answerQuestion(FormIndex index, IAnswerData data) {
        QuestionDef q = model.getQuestionPrompt(index).getQuestion();

        if (model.getEvent(index) != FormEntryController.EVENT_QUESTION) {
            throw new RuntimeException("Non-Question object at the form index.");
        }

        TreeElement element = model.getTreeElement(index);

        // A question is complex when it has a copy tag that needs to be
        // evaluated by copying in the correct xml subtree.  XXX: The code to
        // answer complex questions is incomplete, but luckily this feature is
        // rarely used.
        boolean complexQuestion = q.isComplex();

        boolean hasConstraints = false;

        if (element.isRequired() && data == null) {
            return ANSWER_REQUIRED_BUT_EMPTY;
        }

        if (complexQuestion) {
            if (hasConstraints) {
                //TODO: itemsets: don't currently evaluate constraints for
                //itemset/copy -- haven't figured out how handle it yet
                throw new RuntimeException("Itemsets do not currently evaluate constraints. Your constraint will not work, please remove it before proceeding.");
            } else {
                try {
                    model.getForm().copyItemsetAnswer(q, element, data);
                } catch (InvalidReferenceException ire) {
                    ire.printStackTrace();
                    throw new RuntimeException("Invalid reference while copying itemset answer: " + ire.getMessage());
                }
                q.getActionController().triggerActionsFromEvent(Action.EVENT_QUESTION_VALUE_CHANGED, model.getForm());
                return ANSWER_OK;
            }
        } else {
            if (!model.getForm().evaluateConstraint(index.getReference(), data)) {
                // constraint checking failed
                return ANSWER_CONSTRAINT_VIOLATED;
            }
            commitAnswer(element, index, data);
            q.getActionController().triggerActionsFromEvent(Action.EVENT_QUESTION_VALUE_CHANGED, model.getForm());
            return ANSWER_OK;
        }
    }

    public int checkQuestionConstraint(IAnswerData data) {
        FormIndex index = model.getFormIndex();
        QuestionDef q = model.getQuestionPrompt(index).getQuestion();

        if (model.getEvent(index) != FormEntryController.EVENT_QUESTION) {
            throw new RuntimeException("Non-Question object at the form index.");
        }

        TreeElement element = model.getTreeElement(index);

        if (element.isRequired() && data == null) {
            return ANSWER_REQUIRED_BUT_EMPTY;
        }

        // A question is complex when it has a copy tag that needs to be
        // evaluated by copying in the correct xml subtree.  XXX: The code to
        // answer complex questions is incomplete, but luckily this feature is
        // rarely used.
        boolean complexQuestion = q.isComplex();

        if (complexQuestion) {
            // TODO PLM: unsure how to check constraints of 'complex' questions 
            return ANSWER_OK;
        } else {
            if (!model.getForm().evaluateConstraint(index.getReference(), data)) {
                // constraint checking failed
                return ANSWER_CONSTRAINT_VIOLATED;
            }
            return ANSWER_OK;
        }
    }

    /**
     * saveAnswer attempts to save the current answer into the data model
     * without doing any constraint checking. Only use this if you know what
     * you're doing. For normal form filling you should always use
     * answerQuestion or answerCurrentQuestion.
     *
     * @return true if saved successfully, false otherwise.
     */
    public boolean saveAnswer(FormIndex index, IAnswerData data) {
        if (model.getEvent(index) != FormEntryController.EVENT_QUESTION) {
            throw new RuntimeException("Non-Question object at the form index.");
        }
        TreeElement element = model.getTreeElement(index);
        return commitAnswer(element, index, data);
    }

    /**
     * commitAnswer actually saves the data into the datamodel.
     *
     * @return true if saved successfully, false otherwise
     */
    private boolean commitAnswer(TreeElement element, FormIndex index, IAnswerData data) {
        if (data != null) {
            formEntrySession.addValueSet(index, data.uncast().getString());
        } else {
            formEntrySession.addQuestionSkip(index);
        }

        if (data != null || element.getValue() != null) {
            // we should check if the data to be saved is already the same as
            // the data in the model, but we can't (no IAnswerData.equals())
            model.getForm().setValue(data, index.getReference(), element);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Expand any unexpanded repeats at the given FormIndex.
     */
    public void expandRepeats(FormIndex index) {
        model.createModelIfNecessary(index);
    }


    /**
     * Navigates forward in the form.
     *
     * @return the next event that should be handled by a view.
     */
    public int stepToNextEvent(boolean expandRepeats) {
        return stepEvent(true, expandRepeats);
    }

    public int stepToNextEvent() {
        return stepToNextEvent(true);
    }

    /**
     * Find the FormIndex that comes after the given one.
     */
    public FormIndex getNextIndex(FormIndex index, boolean expandRepeats) {
        return getAdjacentIndex(index, true, expandRepeats);
    }

    /**
     * Find the FormIndex that comes after the given one, expanding any repeats encountered.
     */
    public FormIndex getNextIndex(FormIndex index) {
        return getAdjacentIndex(index, true, true);
    }

    /**
     * Navigates backward in the form.
     *
     * @return the next event that should be handled by a view.
     */
    public int stepToPreviousEvent() {
        // second parameter doesn't matter because stepping backwards never involves descending into repeats
        return stepEvent(false, false);
    }

    /**
     * Moves the current FormIndex to the next/previous relevant position.
     *
     * @param expandRepeats Expand any unexpanded repeat groups
     * @return event associated with the new position
     */
    private int stepEvent(boolean forward, boolean expandRepeats) {
        FormIndex index = model.getFormIndex();
        index = getAdjacentIndex(index, forward, expandRepeats);
        return jumpToIndex(index, expandRepeats);
    }

    /**
     * Find a FormIndex next to the given one.
     *
     * @param forward If true, get the next FormIndex, else get the previous one.
     */
    private FormIndex getAdjacentIndex(FormIndex index, boolean forward, boolean expandRepeats) {
        boolean descend = true;
        boolean relevant;
        boolean inForm;

        do {
            if (forward) {
                index = model.incrementIndex(index, descend);
            } else {
                index = model.decrementIndex(index);
            }

            //reset all step rules
            descend = true;
            relevant = true;
            inForm = index.isInForm();
            if (inForm) {
                relevant = model.isIndexRelevant(index);

                //If this the current index is a group and it is not relevant
                //do _not_ dig into it.
                if (!relevant && model.getEvent(index) == FormEntryController.EVENT_GROUP) {
                    descend = false;
                }
            }
        } while (inForm && !relevant);

        if (expandRepeats) {
            expandRepeats(index);
        }

        return index;
    }

    /**
     * Jumps to a given FormIndex. Expands any repeat groups.
     *
     * @return EVENT for the specified Index.
     */
    public int jumpToIndex(FormIndex index) {
        return jumpToIndex(index, true);
    }

    /**
     * Jumps to a given FormIndex.
     *
     * @param expandRepeats Expand any unexpanded repeat groups
     * @return EVENT for the specified Index.
     */
    public int jumpToIndex(FormIndex index, boolean expandRepeats) {
        model.setQuestionIndex(index, expandRepeats);
        return model.getEvent(index);
    }

    @SuppressWarnings("unused")
    public FormIndex descendIntoNewRepeat() {
        jumpToIndex(model.getForm().descendIntoRepeat(model.getFormIndex(), -1));
        newRepeat(model.getFormIndex());
        return model.getFormIndex();
    }

    @SuppressWarnings("unused")
    public FormIndex descendIntoRepeat(int n) {
        jumpToIndex(model.getForm().descendIntoRepeat(model.getFormIndex(), n));
        return model.getFormIndex();
    }

    /**
     * Creates a new repeated instance of the group referenced by the specified
     * FormIndex.
     */
    public void newRepeat(FormIndex questionIndex) {
        try {
            model.getForm().createNewRepeat(questionIndex);
            formEntrySession.addNewRepeat(questionIndex);
        } catch (InvalidReferenceException ire) {
            throw new RuntimeException("Invalid reference while copying itemset answer: " + ire.getMessage());
        }
    }

    /**
     * Creates a new repeated instance of the group referenced by the current
     * FormIndex.
     */
    public void newRepeat() {
        newRepeat(model.getFormIndex());
    }

    /**
     * Deletes a repeated instance of a group referenced by the specified
     * FormIndex.
     */
    public FormIndex deleteRepeat(FormIndex questionIndex) {
        return model.getForm().deleteRepeat(questionIndex);
    }

    @SuppressWarnings("unused")
    public void deleteRepeat(int n) {
        deleteRepeat(model.getForm().descendIntoRepeat(model.getFormIndex(), n));
    }

    /**
     * Sets the current language.
     */
    public void setLanguage(String language) {
        model.setLanguage(language);
    }

    public String getFormEntrySessionString() {
        return formEntrySession.toString();
    }
}
