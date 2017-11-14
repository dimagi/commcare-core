package org.javarosa.xpath.expr.test;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.test_utils.ClassLoadUtils;
import org.javarosa.test_utils.ExprEvalUtils;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathCustomRuntimeFunc;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testNodesetJoins() {
        FormInstance instance = ExprEvalUtils.loadInstance("/test_xpathpathexpr.xml");
        ExprEvalUtils.testEval("join(' ', /data/places/country/state)", instance, null, "Utah Montana");
        ExprEvalUtils.testEval("join-chunked('-', 5, /data/places/country/state)", instance, null, "UtahM-ontan-a");

    }

        @Test
    public void testSubstringVariants() {
        FormInstance instance = ExprEvalUtils.loadInstance("/test_xpathpathexpr.xml");

        ExprEvalUtils.testEval("substring-before('Utah Montana', /data/places/country/state[2])", instance, null, "Utah ");
        ExprEvalUtils.testEval("substring-before(join(' ', /data/places/country/state), /data/places/country/state[2])", instance, null, "Utah ");
        ExprEvalUtils.testEval("substring-before('hello there', ' there')", instance, null, "hello");
        ExprEvalUtils.testEval("substring-before('hello there there', ' there')", instance, null, "hello");
        ExprEvalUtils.testEval("substring-before('hello there', '')", instance, null, "");
        ExprEvalUtils.testEval("substring-before('hello there', 'foo')", instance, null, "");
        ExprEvalUtils.testEval("substring-before('', 'foo')", instance, null, "");
        ExprEvalUtils.testEval("substring-before('123', 2)", instance, null, "1");

        ExprEvalUtils.testEval("substring-after('Utah Montana', /data/places/country/state[1])", instance, null, " Montana");
        ExprEvalUtils.testEval("substring-after(join(' ', /data/places/country/state), /data/places/country/state[1])", instance, null, " Montana");
        ExprEvalUtils.testEval("substring-after('hello there', 'hello ')", instance, null, "there");
        ExprEvalUtils.testEval("substring-after('hello hello there', 'hello ')", instance, null, "hello there");
        ExprEvalUtils.testEval("substring-after('hello there', '')", instance, null, "hello there");
        ExprEvalUtils.testEval("substring-after('hello there', 'foo')", instance, null, "hello there");
        ExprEvalUtils.testEval("substring-after('', 'foo')", instance, null, "");
        ExprEvalUtils.testEval("substring-after('123', 2)", instance, null, "3");
    }

    @Test
    public void testDistinct() {
        FormInstance instance = ExprEvalUtils.loadInstance("/xpath/test_distinct.xml");

        ExprEvalUtils.testEval("join(' ', distinct-values(/data/places/country/@id))", instance, null, "us ca mx");

        ExprEvalUtils.testEval("join(' ', distinct-values(/data/places/country/@continent))", instance, null, "na");
        ExprEvalUtils.testEval("join(' ', distinct-values(/data/places/country/language))", instance, null, "English Spanish");

        ExprEvalUtils.testEval("join(' ', distinct-values(/data/places/country[language = 'French']/@id))", instance, null, "");
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
        fail("form entry should fail on bad `position` usage before getting here");
    }

    @Test
    public void testCoalesce() {
        FormInstance instance = ExprEvalUtils.loadInstance("/test_xpathpathexpr.xml");

        // demo of basic coalesce behavior
        ExprEvalUtils.testEval("/data/places/country[@id = 'one']/name", instance, null, "Singapore");
        ExprEvalUtils.testEval("/data/places/country[@id = 'three']/name", instance, null, "");
        ExprEvalUtils.testEval("coalesce(/data/places/country[@id = 'three']/name, /data/places/country[@id = 'one']/name)", instance, null, "Singapore");

        // tests for extending coalesce to work with more than one argument
        ExprEvalUtils.testEval("coalesce('', '', /data/places/country[@id = 'one']/name)", instance, null, "Singapore");
        ExprEvalUtils.testEval("coalesce('', /data/places/country[@id = 'three']/name, /data/places/country[@id = 'one']/name)", instance, null, "Singapore");
        ExprEvalUtils.testEval("coalesce('', /data/places/country[@id = 'one']/name, /data/places/country[@id = 'two']/name)", instance, null, "Singapore");
        ExprEvalUtils.testEval("coalesce('', '', '', '', '')", instance, null, "");
    }

    @Test
    public void testCond() {
        FormInstance instance = ExprEvalUtils.loadInstance("/test_xpathpathexpr.xml");
        // test evaluating valid cond statements
        ExprEvalUtils.testEval("cond(true(), 0, 1=1, 1, -1)", instance, null, 0.0);
        ExprEvalUtils.testEval("cond(false(), 0, 1)", instance, null, 1.0);

        // test cond with non-obvious boolean condition
        ExprEvalUtils.testEval("cond('a', 0, 1)", instance, null, 0.0);

        // test nested cond statements
        ExprEvalUtils.testEval("cond(1 = 0, 0, cond(1 = 1, true(), false()), 1, 0)", instance, null, 1.0);

        // test parsing invalid cond statements
        assertParseFailure("cond('a' = 'a')");
        assertParseFailure("cond('a' = 'a', 0)");
        assertParseFailure("cond('a' = 'a', 0, false(), 0)");
        // '*1' is invalid syntax
        assertParseFailure("cond('a', 0, *1)");
    }

    private static void assertParseFailure(String exprString) {
        boolean didParseFail = false;
        try {
            XPathParseTool.parseXPath(exprString);
        } catch (XPathSyntaxException xpse) {
            didParseFail = true;
        }
        assertTrue(didParseFail);
    }

    /**
     * Ensure that static list of xpath functions is kept up-to-date
     */
    @Test
    public void funcListTest() throws ClassNotFoundException, IOException, URISyntaxException {
        Set<Class> cls = ClassLoadUtils.getClasses(XPathFuncExpr.class.getPackage().getName());

        List<Class> funcClasses = ClassLoadUtils.classesThatExtend(cls, XPathFuncExpr.class);

        // exclude functions defined at runtime
        funcClasses.remove(XPathCustomRuntimeFunc.class);

        HashMap<String, Class> funcList = FunctionUtils.getXPathFuncListMap();
        for (Class c : funcClasses) {
            assertTrue(c + " is not in list of functions, please update it",
                    funcList.containsValue(c));
        }
        for (Class c : funcList.values()) {
            assertTrue(c + " is in the list of functions but no longer exists, please remove it.",
                    funcClasses.contains(c));
        }
    }
}
