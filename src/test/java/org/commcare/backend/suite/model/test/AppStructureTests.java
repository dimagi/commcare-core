package org.commcare.backend.suite.model.test;

import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.AssertionSet;
import org.commcare.suite.model.Callout;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.EndpointAction;
import org.commcare.suite.model.GeoOverlay;
import org.commcare.suite.model.Global;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.OfflineUserRestore;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.Text;
import org.commcare.test.utilities.MockApp;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.expr.XPathExpression;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.reactivex.observers.TestObserver;

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
        assertEquals(callout.evaluate(mApp.getSession().getEvaluationContext()).getType(), "text/plain");

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
    public void testDetailGlobalStructure() {
        Global global = mApp.getSession().getPlatform().getDetail("m0_case_short").getGlobal();
        Assert.assertEquals(2, global.getGeoOverlays().length);

        GeoOverlay geoOverlay1 = global.getGeoOverlays()[0];
        Assert.assertEquals("region1", geoOverlay1.getLabel().evaluate().getName());
        Assert.assertEquals(
                "25.099143024399652,76.51193084262178 \\n25.09659806293257,76.50851525117463 \\n25.094815052360374,76.51072357910209 \\n25.097369086424337,76.51234989287263",
                geoOverlay1.getCoordinates().evaluate().getName());

        GeoOverlay geoOverlay2 = global.getGeoOverlays()[1];
        Assert.assertEquals("region2", geoOverlay2.getLabel().evaluate().getName());
        Assert.assertEquals(
                "76.51193084262178,25.099143024399652 \\n76.50851525117463,25.09659806293257 \\n76.51072357910209,25.094815052360374 \\n76.51234989287263,25.097369086424337",
                geoOverlay2.getCoordinates().evaluate().getName());
    }

    @Test
    public void testDetailNoItemsText() {
        Text noItemsText = mApp.getSession().getPlatform().getDetail("m0_case_short").getNoItemsText();
        Assert.assertEquals("Empty List", noItemsText.evaluate());
    }

    @Test
    public void testDemoUserRestoreParsing() throws Exception {
        // Test parsing an app with a properly-formed demo user restore file
        MockApp appWithGoodUserRestore = new MockApp("/app_with_good_demo_restore/");
        OfflineUserRestore offlineUserRestore = appWithGoodUserRestore.getSession().getPlatform()
                .getDemoUserRestore();
        Assert.assertNotNull(offlineUserRestore);
        assertEquals("test", offlineUserRestore.getUsername());

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

    @Test
    public void testDisplayBlockParsing_good() throws Exception {
        MockApp appWithGoodUserRestore = new MockApp("/app_with_good_numeric_badge/");
        Suite s = appWithGoodUserRestore.getSession().getPlatform().getInstalledSuites().get(0);
        Menu menuWithDisplayBlock = s.getMenusWithId("m1").get(0);
        assertEquals("Menu 1 Text", menuWithDisplayBlock.getDisplayText(null));
        EvaluationContext ec =
                appWithGoodUserRestore.getSession().getEvaluationContext(menuWithDisplayBlock.getId());
        TestObserver<String> testObserver = menuWithDisplayBlock.getTextForBadge(ec).test();
        testObserver.assertNoErrors();
        testObserver.assertValue("1");
    }

    @Test
    public void testDisplayBlockParsing_invalidXPathExpr() throws Exception {
        boolean exceptionThrown = false;
        try {
            new MockApp("/app_with_bad_numeric_badge/");
        } catch (UnresolvedResourceException e) {
            exceptionThrown = true;
            String expectedErrorMsg = "Invalid XPath Expression : ,3";
            Assert.assertTrue(
                    "The exception that was thrown was due to an unexpected cause",
                    e.getMessage().contains(expectedErrorMsg));
        }
        if (!exceptionThrown) {
            fail("A Text block of form badge whose xpath element contains an invalid xpath " +
                    "expression should throw an exception");
        }
    }

    @Test
    public void testMenuAssertions() {
        Suite s = mApp.getSession().getPlatform().getInstalledSuites().get(0);
        Menu menuWithAssertionsBlock = s.getMenusWithId("m0").get(0);
        AssertionSet assertions = menuWithAssertionsBlock.getAssertions();
        Assert.assertNotNull(assertions);
    }

    @Test
    public void testDetailWithFieldAction() {
        Detail detail = mApp.getSession().getPlatform().getDetail("m0_case_short");
        DetailField field = detail.getFields()[0];
        EndpointAction endpointAction = field.getEndpointAction();
        assertEquals("case_list",endpointAction.getEndpointId());
        assertEquals(true, endpointAction.isBackground());
    }
}
