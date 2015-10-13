package org.javarosa.core.model.test;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.test.DummyInstanceInitializationFactory;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.test_utils.ExprEvalUtils;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FormDefTest {
    /**
     * Make sure that 'current()' expands correctly when used in conditionals
     * such as in 'relevant' tags. The test answers a question and expects the
     * correct elements to be re-evaluated and set to not relevant.
     */
    @Test
    public void testCurrentFuncInTriggers() {
        FormParseInit fpi = new FormParseInit("/trigger_and_current_tests.xml");

        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null) {
                continue;
            }
            // get the reference of question
            TreeReference qRef = (TreeReference)(q.getBind()).getReference();

            // are we changing the value of /data/show?
            if (qRef.toString().equals("/data/show")) {
                int response = fec.answerQuestion(new StringData("no"));
                if (response != FormEntryController.ANSWER_OK) {
                    fail("Bad response from fec.answerQuestion()");
                }
            } else if (q.getID() == 2) {
                // check (sketchily) if the second question is shown, which
                // shouldn't happen after answering "no" to the first, unless
                // triggers aren't working properly.
                fail("shouldn't be relevant after answering no before");
            }
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }

    /**
     * Make sure that relative references in <bind> elements are correctly
     * contextualized.
     */
    @Test
    public void testRelativeRefInTriggers() {
        FormParseInit fpi = new FormParseInit("/test_nested_preds_with_rel_refs.xml");
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        FormDef fd = fpi.getFormDef();
        // run initialization to ensure xforms-ready event and binds are
        // triggered.
        fd.initialize(true, new DummyInstanceInitializationFactory());

        FormInstance instance = fd.getMainInstance();

        String errorMsg;
        errorMsg = ExprEvalUtils.expectedEval("/data/query-one", instance, null, "0", null);
        assertTrue(errorMsg, "".equals(errorMsg));

        boolean[] shouldBePresent = {true, true};

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null) {
                continue;
            }

            if (q.getID() <= shouldBePresent.length && !shouldBePresent[q.getID() - 1]) {
                fail("question with id " + q.getID() + " shouldn't be relevant");
            }
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }

    @Test
    public void testAnswerConstraint() {
        IntegerData ans = new IntegerData(13);
        FormParseInit fpi = new FormParseInit("/ImageSelectTester.xhtml");
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null || q.getTextID() == null || "".equals(q.getTextID())) {
                continue;
            }
            if (q.getTextID().equals("constraint-test")) {
                int response = fec.answerQuestion(ans);
                if (response == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
                    fail("Answer Constraint test failed.");
                } else if (response == FormEntryController.ANSWER_OK) {
                    break;
                } else {
                    fail("Bad response from fec.answerQuestion()");
                }
            }
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }

    @Test
    public void testAnswerConstraintOldText() {
        IntegerData ans = new IntegerData(7);
        FormParseInit fpi = new FormParseInit("/ImageSelectTester.xhtml");
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());
        fec.setLanguage("English");

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null || q.getTextID() == null || "".equals(q.getTextID())) {
                continue;
            }
            if (q.getTextID().equals("constraint-test")) {
                int response = fec.answerQuestion(ans);
                if (response == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
                    if (!"Old Constraint".equals(fec.getModel().getQuestionPrompt().getConstraintText())) {
                        fail("Old constraint message not found, instead got: "
                                + fec.getModel().getQuestionPrompt().getConstraintText());
                    }
                } else if (response == FormEntryController.ANSWER_OK) {
                    fail("Should have constrained");
                    break;
                }
            }
            if (q.getTextID().equals("constraint-test-2")) {

                int response3 = fec.answerQuestion(new IntegerData(13));
                if (response3 == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
                    if (!"New Alert".equals(fec.getModel().getQuestionPrompt().getConstraintText())) {
                        fail("New constraint message not found, instead got: "
                                + fec.getModel().getQuestionPrompt().getConstraintText());
                    }
                } else if (response3 == FormEntryController.ANSWER_OK) {
                    fail("Should have constrained (2)");
                    break;
                }

            }
            if (q.getTextID().equals("constraint-test-3")) {

                int response4 = fec.answerQuestion(new IntegerData(13));
                if (response4 == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
                    if (!"The best QB of all time: Tom Brady".equals(fec.getModel().getQuestionPrompt().getConstraintText())) {
                        fail("New constraint message not found, instead got: "
                                + fec.getModel().getQuestionPrompt().getConstraintText());
                    }
                } else if (response4 == FormEntryController.ANSWER_OK) {
                    fail("Should have constrained (2)");
                    break;
                }

            }
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }

    /**
     * Test setvalue expressions which have predicate references
     */
    @Test
    public void testSetValuePredicate() {
        FormParseInit fpi = new FormParseInit("/test_setvalue_predicate.xml");
        FormEntryController fec = fpi.getFormEntryController();
        fpi.getFormDef().initialize(true, null);
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        boolean testPassed = false;
        do {
            if (fec.getModel().getEvent() != FormEntryController.EVENT_QUESTION) {
                continue;
            }
            String text = fec.getModel().getQuestionPrompt().getQuestionText();
            //check for our test
            if (text.contains("Test") && text.contains("pass")) {
                testPassed = true;
            }
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
        if (!testPassed) {
            fail("Setvalue Predicate Target Test");
        }
    }

    /**
     * Test nested form repeat triggers and actions
     */
    @Test
    public void testNestedRepeatActions() throws Exception {
        FormParseInit fpi = new FormParseInit("/xform_tests/test_looped_model_iteration.xml");
        FormEntryController fec = fpi.getFormEntryController();
        fpi.getFormDef().initialize(true, null);
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);

        if (!ExprEvalUtils.xpathEvalAndCompare(fpi.getFormDef().getEvaluationContext(), "/data/sum", 30.0)) {
            fail("Nested repeats did not evaluate to the proper outcome");
        }
    }

    @Test
    public void testTriggerCaching() throws Exception {
        FormParseInit fpi = new FormParseInit("/xform_tests/test_trigger_caching.xml");
        FormEntryController fec = fpi.getFormEntryController();
        fpi.getFormDef().initialize(true, null);
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);

        if (!ExprEvalUtils.xpathEvalAndCompare(fpi.getFormDef().getEvaluationContext(), "/data/heaviest_animal_weight", 400.0)) {
            fail("");
        }
        if (!ExprEvalUtils.xpathEvalAndCompare(fpi.getFormDef().getEvaluationContext(), "/data/lightest_animal_weight", 200.0)) {
            fail("");
        }
    }
}
