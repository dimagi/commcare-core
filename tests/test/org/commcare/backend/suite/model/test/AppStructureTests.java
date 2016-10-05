package org.commcare.backend.suite.model.test;

import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Callout;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.OfflineUserRestore;
import org.commcare.suite.model.Text;
import org.commcare.test.utilities.MockApp;
import org.javarosa.xpath.expr.XPathExpression;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        assertEquals("Root Menu Style",
                "grid",
                mApp.getSession().getPlatform().getMenuDisplayStyle("root"));

        assertEquals("Common Menu Style",
                "list",
                mApp.getSession().getPlatform().getMenuDisplayStyle("m1"));

        assertEquals("Disperate Menu Style",
                null,
                mApp.getSession().getPlatform().getMenuDisplayStyle("m2"));

        assertEquals("Empty Menu",
                null,
                mApp.getSession().getPlatform().getMenuDisplayStyle("m0"));

        assertEquals("Specific override",
                "grid",
                mApp.getSession().getPlatform().getMenuDisplayStyle("m3"));
    }

    @Test
    public void testDetailStructure() {
        // A suite detail can have a lookup block for performing an app callout
        Callout callout = 
            mApp.getSession().getPlatform().getDetail("m0_case_short").getCallout();

        // specifies the callout's intent type
        assertEquals(callout.getRawCalloutData().getType(), "text/plain");

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
    public void testDemoUserRestoreParsing() throws Exception {
        // Test parsing an app with a properly-formed demo user restore file
        MockApp appWithGoodUserRestore = new MockApp("/app_with_good_demo_restore/");
        OfflineUserRestore offlineUserRestore = appWithGoodUserRestore.getSession().getPlatform()
                .getDemoUserRestore();
        Assert.assertNotNull(offlineUserRestore);
        assertEquals("test", offlineUserRestore.getUsername());
        Assert.assertNotNull(offlineUserRestore.getPassword());

        // Test parsing an app where the user_type is not set to 'demo'
        boolean exceptionThrown = false;
        try {
            new MockApp("/app_with_bad_demo_restore/");
        } catch (UnresolvedResourceException e) {
            exceptionThrown = true;
            String expectedErrorMsg =
                    "Demo user restore file must be for a user with user_type set to demo";
            assertEquals(
                    "The UnresolvedResourceException that was thrown was due to an unexpected cause, " +
                            "the actual error message is: " + e.getMessage(),
                    expectedErrorMsg,
                    e.getMessage());
        }
        if (!exceptionThrown) {
            fail("A demo user restore file that does not specify user_type to demo should throw " +
                    "an UnfulfilledRequirementsException");
        }

        // Test parsing an app where the username block is empty
        exceptionThrown = false;
        try {
            new MockApp("/app_with_bad_demo_restore2/");
        } catch (UnresolvedResourceException e) {
            exceptionThrown = true;
            String expectedErrorMsg =
                    "Demo user restore file must specify a username in the Registration block";
            assertEquals(
                    "The UnresolvedResourceException that was thrown was due to an unexpected cause, " +
                            "the actual error message is: " + e.getMessage(),
                    expectedErrorMsg,
                    e.getMessage());
        }
        if (!exceptionThrown) {
            fail("A demo user restore file that does not specify a username should throw " +
                    "an UnfulfilledRequirementsException");
        }
    }
}
