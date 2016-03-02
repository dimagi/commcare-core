package org.javarosa.xpath.expr.test;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.test_utils.ExprEvalUtils;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for xpath functions
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class XPathFuncExprTest {
    @Test
    public void testOutOfBoundsSelect() {
        FormInstance instance = ExprEvalUtils.loadInstance("/test_xpathpathexpr.xml");

        // normal selected-at behaviour
        ExprEvalUtils.testEval("selected-at('hello there', 0)", instance, null, "hello");
        ExprEvalUtils.testEval("selected-at('hello there', 1)", instance, null, "there");

        // out of bounds selection should raise an XPathException
        ExprEvalUtils.testEval("selected-at('hello there', 2)", instance, null, new XPathException());
    }

    /**
     * Test that `position(some_ref)` throws a XPathTypeMismatchException when
     * some_ref points to an empty nodeset
     */
    @Test
    public void testOutOfBoundsPosition() throws XPathSyntaxException {
        FormParseInit fpi = new FormParseInit("/xform_tests/test_position_with_ref.xml");
        FormEntryController fec = fpi.getFormEntryController();
        fpi.getFormDef().initialize(true, null);
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        try {
            do {
            } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
        } catch (XPathTypeMismatchException e) {
            return;
        }
        Assert.fail("form entry should fail on bad `position` usage before getting here");
    }
}
