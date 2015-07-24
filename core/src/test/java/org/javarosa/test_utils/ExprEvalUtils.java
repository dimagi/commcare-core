package org.javarosa.test_utils;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * Commonly used utilities for evaluating xpath expressions.
 *
 * @author Phillip Mates
 */

public class ExprEvalUtils {
    private static final double DOUBLE_TOLERANCE = 1.0e-12;

    /**
     * Parse and evaluate an xpath expression and check that the result matches
     * the designated expected result.
     *
     * @param rawExpr   Unparsed xpath expression
     * @param model     Representation of form DOM used to evaluate expression
     *                  against.
     * @param evalCtx   Particular context that the expression should be
     *                  evaluated. If null, build from the model argument.
     * @param expected  The expected result of evaluating the xpath expression.
     *                  Can be an exception object.
     * @param tolerance Acceptable numerical difference in expected and
     *                  resulting values. If null, use the default tolerance.
     * @return Empty string if the result of evaluating the expression is
     * equivalent to the expected value. Otherwise, an error message detailing
     * what went wrong.
     */
    public static String expectedEval(String rawExpr, FormInstance model,
                                      EvaluationContext evalCtx, Object expected,
                                      Double tolerance) {
        XPathExpression expr;
        boolean exceptionExpected = (expected instanceof XPathException);

        if (tolerance == null) {
            tolerance = DOUBLE_TOLERANCE;
        }

        if (evalCtx == null) {
            evalCtx = new EvaluationContext(model);
        }

        try {
            expr = XPathParseTool.parseXPath(rawExpr);
        } catch (XPathSyntaxException xpse) {
            return "Parsing syntax error for " + rawExpr;
        }

        if (expr == null) {
            return "Parsing " + rawExpr + " in a null expression.";
        }

        try {
            Object result = XPathFuncExpr.unpack(expr.eval(model, evalCtx));

            if (exceptionExpected) {
                return "Expected exception, expression : " + rawExpr;
            } else if ((result instanceof Double && expected instanceof Double)) {
                if (Math.abs((Double)result - (Double)expected) > tolerance) {
                    return "Doubles outside of tolerance: got " +
                            result + ", expected " + expected;
                }
            } else if (!expected.equals(result)) {
                return "Expected " + expected + ", got " + result;
            }
        } catch (XPathException xpathEx) {
            if (!exceptionExpected) {
                return "Did not expect " + xpathEx.getClass() + " exception";
            } else if (xpathEx.getClass() != expected.getClass()) {
                return "Did not get expected exception type";
            }
        }
        return "";
    }
}
