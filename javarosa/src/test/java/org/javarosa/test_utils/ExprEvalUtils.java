package org.javarosa.test_utils;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.utils.FormLoadingUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;

import java.io.IOException;

import static org.junit.Assert.fail;

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
            Object result = FunctionUtils.unpack(expr.eval(model, evalCtx));

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

    public static void assertEqualsXpathEval(String failureMessage,
                                             Object expectedOutput,
                                             String input,
                                             EvaluationContext evalContext)
            throws XPathSyntaxException {
        Object evalResult = xpathEval(evalContext, input);
        Assert.assertEquals(failureMessage, expectedOutput, evalResult);
    }

    // Evaluated expression must be castable to Double
    public static void assertAlmostEqualsXpathEval(Double expectedOutput,
                                                   Double tolerance,
                                                   String input,
                                                   EvaluationContext evalContext)
            throws XPathSyntaxException {
        Double evalResult = (Double) xpathEval(evalContext, input);
        Double difference = Math.abs(evalResult - expectedOutput);
        Assert.assertTrue("Evaluated result and expected output differ by " + difference,
                difference < tolerance);
    }

    public static Object xpathEval(EvaluationContext evalContext,
                                   String input)
            throws XPathSyntaxException {
        XPathExpression expr;
        expr = XPathParseTool.parseXPath(input);
        return FunctionUtils.unpack(expr.eval(evalContext));
    }

    public static void testEval(String expr, EvaluationContext ec, Object expected) {
        testEval(expr, null, ec, expected, 1.0e-12);
    }

    public static void testEval(String expr, FormInstance model, EvaluationContext ec, Object expected) {
        testEval(expr, model, ec, expected, 1.0e-12);
    }

    public static void testEval(String expr, FormInstance model, EvaluationContext ec, Object expected, double tolerance) {
        XPathExpression xpe = null;
        boolean exceptionExpected = (expected instanceof XPathException);

        if (ec == null) {
            ec = new EvaluationContext(model);
        }

        try {
            xpe = XPathParseTool.parseXPath(expr);
        } catch (XPathSyntaxException xpse) {
        }

        if (xpe == null) {
            fail("Null expression or syntax error " + expr);
        }

        try {
            Object result = FunctionUtils.unpack(xpe.eval(model, ec));
            if (tolerance != DOUBLE_TOLERANCE) {
                System.out.println(expr + " = " + result);
            }

            if (exceptionExpected) {
                fail("Expected exception, expression : " + expr);
            } else if ((result instanceof Double && expected instanceof Double)) {
                Double o = (Double)result;
                Double t = (Double)expected;
                if (Math.abs(o - t) > tolerance) {
                    fail("Doubles outside of tolerance: got " + o + ", expected " + t);
                }
            } else if (!expected.equals(result)) {
                fail("Expected " + expected + ", got " + result);
            }
        } catch (XPathException xpex) {
            if (!exceptionExpected) {
                xpex.printStackTrace();
                fail("Did not expect " + xpex.getClass() + " exception");
            } else if (xpex.getClass() != expected.getClass()) {
                fail("Did not get expected exception type");
            }
        }
    }

    /**
     * Load a form instance from a path.
     * Doesn't create a model or main instance.
     *
     * @param formPath path of the form to load, relative to project build
     * @return FormInstance created from the path pointed to, or null if any
     * error occurs.
     */
    public static FormInstance loadInstance(String formPath) {
        FormInstance instance = null;
        try {
            instance = FormLoadingUtils.loadFormInstance(formPath);
        } catch (IOException e) {
            fail("Unable to load form at " + formPath);
        } catch (InvalidStructureException e) {
            fail("Form at " + formPath + " has an invalid structure.");
        }
        return instance;
    }
}
