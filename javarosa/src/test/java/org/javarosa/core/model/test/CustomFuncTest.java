package org.javarosa.core.model.test;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathUnhandledException;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Will Pride
 */
public class CustomFuncTest {
    private FormParseInit fpi;

    private final double errorDelta = 0.001;

    /**
     * Try to use a form that has a custom function defined without extending
     * the context with a custom function handler.
     */
    @Test
    public void testFormFailure() {
        fpi = new FormParseInit("/CustomFunctionTest.xhtml");

        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null) {
                continue;
            }

            try {
                fec.answerQuestion(new IntegerData(1));
            } catch (XPathUnhandledException e) {
                // we expect the test to fail on parsing
                return;
            }
            fail("Should have failed parsing here");
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }

    /**
     * Successfully use a form that has a custom function by extending the
     * context with a custom function handler.
     */
    @Test
    public void testFormSuccess() {
        fpi = new FormParseInit("/CustomFunctionTest.xhtml");

        // Custom func to double the numeric argument passed in.
        IFunctionHandler myDouble = new IFunctionHandler() {
            @Override
            public String getName() {
                return "my_double";
            }

            @Override
            public Object eval(Object[] args, EvaluationContext ec) {
                Double my_double = (Double)args[0];
                assertEquals(2.0, my_double * 2.0, errorDelta);
                return my_double * 2.0;
            }

            @Override
            public Vector getPrototypes() {
                Class[] proto = {Double.class};
                Vector<Class[]> v = new Vector<>();
                v.addElement(proto);
                return v;
            }

            @Override
            public boolean rawArgs() {
                return false;
            }
        };

        fpi.getFormDef().exprEvalContext.addFunctionHandler(myDouble);

        FormEntryController fec = fpi.getFormEntryController();

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null) {
                continue;
            }
            fec.answerQuestion(new IntegerData(1));
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }
}
