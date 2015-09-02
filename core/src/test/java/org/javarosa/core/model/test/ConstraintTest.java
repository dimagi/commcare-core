package org.javarosa.core.model.test;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.condition.pivot.IntegerRangeHint;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author William Pride (wpride@dimagi.com)
 */
public class ConstraintTest {
    private FormParseInit fpi;

    /**
     * load and parse form
     */
    @Before
    public void initForm() {
        System.out.println("init Constraint Test");
        fpi = new FormParseInit("/test_constraints.xml");
    }

    /**
     * Tests constraint passing and failing when using FormEntryController to
     * answer form questions.
     *
     * TODO: create test cases that test complex questions, that is, those with
     * copy tags inside of them that need to processed.
     */
    @Test
    public void testAnswerQuestion() {
        IntegerData ans;
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
            // get current question
            QuestionDef q = fpi.getCurrentQuestion();

            if (q == null || q.getTextID() == null || q.getTextID() == "") {
                continue;
            }

            if (q.getTextID().equals("constraint-max-label")){
                assertConstraintMaxMin(new Integer(30), null);
            } else if (q.getTextID().equals("constraint-min-label")){
                assertConstraintMaxMin(null, new Integer(10));
            } else if (q.getTextID().equals("constraint-max-or-min-label")){
                assertUnpivotable();
            } else if (q.getTextID().equals("constraint-max-and-min-label")){
                assertUnpivotable();
            }
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }

    private void assertConstraintMaxMin(Integer max, Integer min){
        IntegerRangeHint hint = new IntegerRangeHint();
        FormEntryPrompt prompt = fpi.getFormEntryModel().getQuestionPrompt();
        try {
            prompt.requestConstraintHint(hint);

            if(max != null) {
                assert (max.equals(hint.getMax().getValue()));
            } else{
                assert(hint.getMax() == null);
            }

            if(min != null) {
                assert (min.equals(hint.getMin().getValue()));
            } else{
                assert(hint.getMin() == null);
            }
        } catch (UnpivotableExpressionException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void assertUnpivotable(){
        IntegerRangeHint hint = new IntegerRangeHint();
        FormEntryPrompt prompt = fpi.getFormEntryModel().getQuestionPrompt();
        try {
            prompt.requestConstraintHint(hint);
            fail("Should have not been able to pivot with prompt: " + prompt);
        } catch (UnpivotableExpressionException e) {
            // good
        }
    }
}
