/*
 * Copyright (C) 2015 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.form.api.test;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import org.javarosa.core.model.FormElementStateListener;
import org.javarosa.core.model.FormIndex;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

/**
 * @author Phillip Mates
 */
public class FormEntryControllerTest extends TestCase {
    private FormParseInit fpi;
    private FormEntryController fec;

    public FormEntryControllerTest() {
        super();
        initForm();
    }

    public FormEntryControllerTest(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
        initForm();
    }

    /**
     * load and parse form
     */
    private void initForm() {
        System.out.println("init FormEntryControllerTest");
        fpi = new FormParseInit("/test_form_entry_controller.xml");
    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();

        aSuite.addTest(new FormEntryControllerTest("FormEntryController Test answerQuestion method", new TestMethod() {
            public void run(TestCase tc) {
                ((FormEntryControllerTest)tc).testAnswerQuestion();
            }
        }));

        return aSuite;
    }

    /**
     * Tests constraint passing and failing when using FormEntryController to
     * answer form questions.
     *
     * TODO: create test cases that test complex questions, that is, those with
     * copy tags inside of them that need to processed.
     */
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


            if (q.getTextID().equals("select-without-constraint-label")) {
                ans = new IntegerData(20);
                expectToPassConstraint(fec.answerQuestion(ans), q.getTextID(), ans.getDisplayText());
            } else if (q.getTextID().equals("select-with-constraint-pass-label")) {
                ans = new IntegerData(10);
                expectToPassConstraint(fec.answerQuestion(ans), q.getTextID(), ans.getDisplayText());
            } else if (q.getTextID().equals("select-with-constraint-fail-label")) {
                ans = new IntegerData(31);
                expectToFailConstraint(fec.answerQuestion(ans), q.getTextID(), ans.getDisplayText());
            } else if (q.getTextID().equals("simple-without-constraint-label")) {
                ans = new IntegerData(40);
                expectToPassConstraint(fec.answerQuestion(ans), q.getTextID(), ans.getDisplayText());
            } else if (q.getTextID().equals("simple-with-constraint-pass-label")) {
                ans = new IntegerData(5);
                expectToPassConstraint(fec.answerQuestion(ans), q.getTextID(), ans.getDisplayText());
            } else if (q.getTextID().equals("simple-with-constraint-fail-label")) {
                ans = new IntegerData(15);
                expectToFailConstraint(fec.answerQuestion(ans), q.getTextID(), ans.getDisplayText());
            }
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }

    /**
     * Check for response code signalling that the answer to a question passed
     * its constraint. Throw a useful error message if this isn't the case.
     *
     * @param responseCode   code returned from FormEntryController after answering a question
     * @param questionText   the descriptor text of the question answered
     * @param answerAsString a string representation of the answer value for the question
     */
    private void expectToPassConstraint(int responseCode, String questionText, String answerAsString) {
        if (responseCode == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
            fail("Answered question with a value that didn't pass its constraint: \n" +
                    "[Question] = " + questionText + " \n" +
                    "[Answer] = " + answerAsString);
        } else if (responseCode != FormEntryController.ANSWER_OK) {
            fail("Unexpected response from FormEntryController.answerQuestion(): \n" +
                    "[Response Code] = " + Integer.toString(responseCode) + " \n" +
                    "[Question] = " + questionText + " \n" +
                    "[Answer] = " + answerAsString);
        }
    }

    /**
     * Check for response code signalling that the answer to a question failed
     * its constraint. Throw a useful error message if this isn't the case.
     *
     * @param responseCode   code returned from FormEntryController after answering a question
     * @param questionText   the descriptor text of the question answered
     * @param answerAsString a string representation of the answer value for the question
     */
    private void expectToFailConstraint(int responseCode, String questionText, String answerAsString) {
        if (responseCode == FormEntryController.ANSWER_OK) {
            fail("Answered question with a value that should have failed the question's constraint: \n" +
                    "[Question] = " + questionText + " \n" +
                    "[Answer] = " + answerAsString);
        } else if (responseCode != FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
            fail("Unexpected response from FormEntryController.answerQuestion(): \n" +
                    "[Response Code] = " + Integer.toString(responseCode) + " \n" +
                    "[Question] = " + questionText + " \n" +
                    "[Answer] = " + answerAsString);
        }
    }
}
