package org.commcare.backend.suite.model.test;

import org.commcare.suite.model.Callout;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.Text;
import org.commcare.test.utilities.MockApp;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.expr.XPathExpression;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for general app structure, like menus and commands
 *
 * @author ctsims
 */
public class AppStructureTests {
    private MockApp mApp;

    @Before
    public void setUp() throws Exception {
        mApp = new MockApp("/app_structure/");
    }

    @Test
    public void testMenuStyles() {
        Assert.assertEquals("Root Menu Style",
                "grid",
                mApp.getSession().getPlatform().getMenuDisplayStyle("root"));

        Assert.assertEquals("Common Menu Style",
                "list",
                mApp.getSession().getPlatform().getMenuDisplayStyle("m1"));

        Assert.assertEquals("Disperate Menu Style",
                null,
                mApp.getSession().getPlatform().getMenuDisplayStyle("m2"));

        Assert.assertEquals("Empty Menu",
                null,
                mApp.getSession().getPlatform().getMenuDisplayStyle("m0"));

        Assert.assertEquals("Specific override",
                "grid",
                mApp.getSession().getPlatform().getMenuDisplayStyle("m3"));
    }

    @Test
    public void testDetailStructure() {
        // A suite detail can have a lookup block for performing an app callout
        Callout callout = 
            mApp.getSession().getPlatform().getDetail("m0_case_short").getCallout();

        // specifies the callout's intent type
        Assert.assertEquals(callout.getRawCalloutData().getType(), "text/plain");

        // If the detail block represents an entity list, then the 'lookup' can
        // have a detail field describing the UI for displaying callout result
        // data in the case list.
        DetailField lookupCalloutDetailField = callout.getResponseDetailField();

        // The header is the data's title
        Assert.assertTrue(lookupCalloutDetailField.getHeader() != null);

        // The template defines the key used to map an entity to the callout
        // result data.  callout result data is a mapping from keys to string
        // values, so each entity who's template evalutates to a key will have
        // the associated result data attached to it.
        Assert.assertTrue(lookupCalloutDetailField.getTemplate() instanceof Text);
    }

    @Test
    public void testDetailWithFocusFunction() {
        XPathExpression focusFunction =
                mApp.getSession().getPlatform().getDetail("m1_case_short").getFocusFunction();
        Assert.assertTrue(focusFunction != null);
    }

    @Test
    public void testDetailWithoutFocusFunction() {
        XPathExpression focusFunction =
                mApp.getSession().getPlatform().getDetail("m0_case_short").getFocusFunction();
        Assert.assertTrue(focusFunction == null);
    }

    @Test
    public void testDemoUserRestoreParsing() {

    }
}
