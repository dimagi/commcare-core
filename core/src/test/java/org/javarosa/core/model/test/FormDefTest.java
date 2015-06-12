package org.javarosa.core.model.test;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.model.xform.XPathReference;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Phillip Mates
 */
public class FormDefTest extends TestCase {

    // How many tests does the suite have?
    // Used to dispatch in doTest's switch statement.

    public FormDefTest(String name) {
        super(name);
    }

    public FormDefTest() {
        super();
    }

    public Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(new FormDefTest("testAnswerConstraint");
        suite.addTest(new FormDefTest("testCurrentFuncInTriggers");
        suite.addTest(new FormDefTest("testAnswerConstraintOldText");
        suite.addTest(new FormDefTest("testSetValuePredicate");

        return suite;
    }



    /**
     * Make sure that 'current()' expands correctly when used in conditionals
     * such as in 'relevant' tags. The test answers a question and expects the
     * correct elements to be re-evaluated and set to not relevant.
     */
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
            TreeReference qRef = (TreeReference)((XPathReference)q.getBind()).getReference();

            // are we changing the value of /data/show?
            if (qRef.toString().equals("/data/show")) {
                int response = fec.answerQuestion(new StringData("no"));
                if (response != fec.ANSWER_OK) {
                    fail("Bad response from fec.answerQuestion()");
                }
            } else if (q.getID() == 2) {
                // check (sketchily) if the second question is shown, which
                // shouldn't happen after answering "no" to the first, unless
                // triggers aren't working properly.
                fail("shouldn't be relevant after answering no before");
            }
        } while (fec.stepToNextEvent() != fec.EVENT_END_OF_FORM);
    }

    public void testAnswerConstraint() {
        IntegerData ans = new IntegerData(13);
        FormParseInit fpi = new FormParseInit("/ImageSelectTester.xhtml");
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null || q.getTextID() == null || q.getTextID() == "") {
                continue;
            }
            if (q.getTextID().equals("constraint-test")) {
                int response = fec.answerQuestion(ans);
                if (response == fec.ANSWER_CONSTRAINT_VIOLATED) {
                    fail("Answer Constraint test failed.");
                } else if (response == fec.ANSWER_OK) {
                    break;
                } else {
                    fail("Bad response from fec.answerQuestion()");
                }
            }
        } while (fec.stepToNextEvent() != fec.EVENT_END_OF_FORM);
    }

    public void testAnswerConstraintOldText() {
        IntegerData ans = new IntegerData(7);
        FormParseInit fpi = new FormParseInit("/ImageSelectTester.xhtml");
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());
        fec.setLanguage("English");

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null || q.getTextID() == null || q.getTextID() == "") {
                continue;
            }
            if (q.getTextID().equals("constraint-test")) {
                int response = fec.answerQuestion(ans);
                if (response == fec.ANSWER_CONSTRAINT_VIOLATED) {
                    if (!"Old Constraint".equals(fec.getModel().getQuestionPrompt().getConstraintText())) {
                        fail("Old constraint message not found, instead got: "
                                + fec.getModel().getQuestionPrompt().getConstraintText());
                    }
                } else if (response == fec.ANSWER_OK) {
                    fail("Should have constrained");
                    break;
                }
            }
            if (q.getTextID().equals("constraint-test-2")) {

                int response3 = fec.answerQuestion(new IntegerData(13));
                if (response3 == fec.ANSWER_CONSTRAINT_VIOLATED) {
                    if(!"New Alert".equals(fec.getModel().getQuestionPrompt().getConstraintText())){
                        fail("New constraint message not found, instead got: "
                                + fec.getModel().getQuestionPrompt().getConstraintText());
                    }
                } else if (response3 == fec.ANSWER_OK) {
                    fail("Should have constrained (2)");
                    break;
                }

            }
            if (q.getTextID().equals("constraint-test-3")) {

                int response4 = fec.answerQuestion(new IntegerData(13));
                if (response4 == fec.ANSWER_CONSTRAINT_VIOLATED) {
                    if(!"The best QB of all time: Tom Brady".equals(fec.getModel().getQuestionPrompt().getConstraintText())){
                        fail("New constraint message not found, instead got: "
                                + fec.getModel().getQuestionPrompt().getConstraintText());
                    }
                } else if (response4 == fec.ANSWER_OK) {
                    fail("Should have constrained (2)");
                    break;
                }

            }
        } while (fec.stepToNextEvent() != fec.EVENT_END_OF_FORM);
    }

    /**
     * Test setvalue expressions which have predicate references
     */
    public void testSetValuePredicate() {
        FormParseInit fpi = new FormParseInit("/test_setvalue_predicate.xml");
        FormEntryController fec = fpi.getFormEntryController();
        fpi.getFormDef().initialize(true,null);
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        boolean testPassed = false;
        do {
            if(fec.getModel().getEvent() != FormEntryController.EVENT_QUESTION) {
                continue;
            }
            String text = fec.getModel().getQuestionPrompt().getQuestionText();
            //check for our test
            if(text.indexOf("Test") != -1) {
                if(text.indexOf("pass") != -1) {
                    testPassed = true;
                }
            }
            
        } while (fec.stepToNextEvent() != fec.EVENT_END_OF_FORM);
        if(!testPassed) {
            fail("Setvalue Predicate Target Test");
        }
    }
}
