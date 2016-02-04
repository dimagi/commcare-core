package org.javarosa.core.model.test;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.test.DummyInstanceInitializationFactory;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.test_utils.ExprEvalUtils;
import org.junit.Test;

import java.util.Date;

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
        FormEntryController fec = initFormEntry(fpi);

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
        FormParseInit fpi = new FormParseInit("/ImageSelectTester.xhtml");
        FormEntryController fec = initFormEntry(fpi);

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null || q.getTextID() == null || "".equals(q.getTextID())) {
                continue;
            }
            if (q.getTextID().equals("constraint-test")) {
                int response = fec.answerQuestion(new IntegerData(13));
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
        FormEntryController fec = initFormEntry(fpi);
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
        FormEntryController fec = initFormEntry(fpi);

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
        FormEntryController fec = initFormEntry(fpi);
        stepThroughEntireForm(fec);

        ExprEvalUtils.assertEqualsXpathEval("Nested repeats did not evaluate to the proper outcome",
                30.0,
                "/data/sum",
                fpi.getFormDef().getEvaluationContext());
    }

    /**
     * Test triggers fired from inserting a new repeat entry. Triggers fired
     * during insert action don't need to be fired again when all triggers
     * rooted by that repeat entry are fired.
     */
    @Test
    public void testRepeatInsertTriggering() throws Exception {
        FormParseInit fpi =
                new FormParseInit("/xform_tests/test_repeat_insert_duplicate_triggering.xml");
        FormEntryController fec = initFormEntry(fpi);
        stepThroughEntireForm(fec);

        EvaluationContext evalCtx = fpi.getFormDef().getEvaluationContext();
        // make sure the language isn't the default language, 'esperanto',
        // which it is initially set to
        ExprEvalUtils.assertEqualsXpathEval("Check language set correctly",
                "en", "/data/iter/country[1]/language", evalCtx);
        ExprEvalUtils.assertEqualsXpathEval("Check id attr set correctly",
                "1", "/data/iter/country[2]/@id", evalCtx);
        ExprEvalUtils.assertEqualsXpathEval("Check id node set correctly",
                "1", "/data/iter/country[2]/id", evalCtx);
    }

    @Test
    public void testQuestionLevelAction_timeStamp() throws Exception {
        FormParseInit fpi =
                new FormParseInit("/xform_tests/test_question_level_actions.xml");
        FormEntryController fec = initFormEntry(fpi);

        int questionIndex = 0;
        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null) {
                continue;
            }

            // Note this relies on the questions in the test xml file staying in the current order
            if (questionIndex == 0) {
                fec.answerQuestion(new StringData("Answer to text question"));
            } else if (questionIndex == 1) {
                fec.answerQuestion(new SelectOneData(new Selection("one")));
            }

            questionIndex++;
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);

        EvaluationContext evalCtx = fpi.getFormDef().getEvaluationContext();
        Object evalResult = ExprEvalUtils.xpathEval(evalCtx, "/data/text/@time");
        assertTrue(evalResult.getClass().equals(Date.class));
        /*ExprEvalUtils.assertEqualsXpathEval("Check that a timestamp was set for the text question",
                "en", "/data/text/@time", evalCtx);
        ExprEvalUtils.assertEqualsXpathEval("Check that a timestamp was set for the select question",
                "en", "/data/selection/@time", evalCtx);*/
    }

    /**
     * Tests trigger caching related to cascading relevancy calculations to children.
     */
    @Test
    public void testTriggerCaching() throws Exception {
        // Running the form creates a few animals with weights that count down from the init_weight.
        // Skips over a specified entry by setting it to irrelevant.
        FormParseInit fpi = new FormParseInit("/xform_tests/test_trigger_caching.xml");
        FormEntryController fec = initFormEntry(fpi);
        stepThroughEntireForm(fec);

        EvaluationContext evalCtx = fpi.getFormDef().getEvaluationContext();
        ExprEvalUtils.assertEqualsXpathEval("Check max animal weight",
                400.0, "/data/heaviest_animal_weight", evalCtx);
        ExprEvalUtils.assertEqualsXpathEval("Check min animal",
                100.0, "/data/lightest_animal_weight", evalCtx);
        ExprEvalUtils.assertEqualsXpathEval("Ensure we skip over setting attr of irrelevant entry",
                "", "/data/animals[/data/skip_weighing_nth_animal]/weight/@time", evalCtx);

        Object weighTimeResult =
                ExprEvalUtils.xpathEval(evalCtx,
                        "/data/animals[/data/skip_weighing_nth_animal - 1]/weight/@time");
        if ("".equals(weighTimeResult) || "-1".equals(weighTimeResult)) {
            fail("@time should be set for relevant animal weight.");
        }
        ExprEvalUtils.assertEqualsXpathEval("Assert genus skip value",
                1.0, "/data/skip_genus_nth_animal", evalCtx);
        ExprEvalUtils.assertEqualsXpathEval("Ensure genus at skip entry is irrelevant",
                "", "/data/animals[1]/genus/species", evalCtx);
        ExprEvalUtils.assertEqualsXpathEval("Ensure genuse at non-skip entry has default value",
                "default", "/data/animals[2]/genus/species", evalCtx);

        ExprEvalUtils.assertEqualsXpathEval(
                "Relevancy of skipped genus entry should be irrelevant to, due to the way it is calculated",
                "", "/data/disabled_species", evalCtx);
    }

    /**
     * Regressions around complex repeat behaviors
     */
    @Test
    public void testLoopedRepeatIndexFetches() throws Exception {
        FormParseInit fpi = new FormParseInit("/xform_tests/test_looped_form_index_fetch.xml");
        FormEntryController fec = initFormEntry(fpi);

        fec.stepToNextEvent();
        fec.stepToNextEvent();

        fec.answerQuestion(new IntegerData(2));
        while(fec.stepToNextEvent() != FormEntryController.EVENT_QUESTION);

        fec.answerQuestion(new UncastData("yes"));
        while(fec.stepToNextEvent() != FormEntryController.EVENT_QUESTION) ;

        fec.getNextIndex(fec.getModel().getFormIndex(), true);
        fec.answerQuestion(new IntegerData(2));
        fec.getNextIndex(fec.getModel().getFormIndex(), true);
    }

    private static void stepThroughEntireForm(FormEntryController fec) {
        do {
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }

    private static FormEntryController initFormEntry(FormParseInit fpi) {
        FormEntryController fec = fpi.getFormEntryController();
        fpi.getFormDef().initialize(true, null);
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());
        return fec;
    }

}
