package org.commcare;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.test.utilities.MockApp;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.test_utils.ExprEvalUtils;
import org.junit.Test;

import java.util.Vector;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class RefreshBug {

    @Test
    public void testFormEntry() throws Exception  {
        MockApp mockApp = new MockApp("/question-refresh-bug/");
        SessionWrapper session = mockApp.getSession();
        session.setCommand("m0-f0");
        session.setDatum("case_id", "44beba8cca3847808d83e0e47fce192b");

        FormParseInit fpi = mockApp.loadAndInitForm("form.xml");
        FormEntryController fec = fpi.getFormEntryController();
        IAnswerData ans;

        do {
            // get current question
            QuestionDef q = fpi.getCurrentQuestion();

            if (q == null || q.getTextID() == null || "".equals(q.getTextID())) {
                continue;
            }

            if (q.getTextID().equals("entrepreneur-owes-teuk-saat/pay-in-full-label")) {
                ans = new SelectOneData(new Selection("no"));
                fec.answerQuestion(ans);
            } else if (q.getTextID().equals("entrepreneur-owes-teuk-saat/amount-to-pay-label")) {
                ans = new IntegerData(1900);
                fec.answerQuestion(ans);
            } else if (q.getTextID().equals("entrepreneur-owes-teuk-saat/question2/invoices_to_pay-label")) {
                Vector<Selection> selections = new Vector<>();

                selections.add(new Selection("9b47f2c2e56546979a97f8324c12ee41"));
                selections.add(new Selection("0bd358897258408888cdaec6f36fc424"));
                selections.add(new Selection("a6c58e5d7ec44367bc264962778b38ac"));
                selections.add(new Selection("dfeb3969979e4cfdbb68f0d2d95baad4"));
                ans = new SelectMultiData(selections);
                fec.answerQuestion(ans);
                fec.answerQuestion(ans);

                // the sum of all the 'invoice' cases
                ExprEvalUtils.testEval("/data/total-amount-owed-to-teuk-saat",
                        fpi.getFormDef().getInstance(), null, 3100.0);
                ExprEvalUtils.testEval("sum(/data/invoice/item/amount-owed-to-teuk-saat)",
                        fpi.getFormDef().getInstance(), null, 3100.0);
                ExprEvalUtils.testEval("data/entrepreneur-owes-teuk-saat/total-to-pay",
                        fpi.getFormDef().getInstance(), null, 2800.0);
                ExprEvalUtils.testEval("/data/entrepreneur-owes-teuk-saat/remaining-funds",
                        fpi.getFormDef().getInstance(), null, -300.0);
            }
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }
}
