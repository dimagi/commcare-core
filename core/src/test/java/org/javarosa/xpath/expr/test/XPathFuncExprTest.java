package org.javarosa.xpath.expr.test;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.test_utils.ExprEvalUtils;
import org.javarosa.xpath.XPathException;
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
}
