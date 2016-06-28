package org.commcare.backend.session.test;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.test.utilities.MockApp;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.test_utils.ExprEvalUtils;
import org.javarosa.xpath.XPathMissingInstanceException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;

import org.commcare.session.SessionFrame;
import org.junit.Test;

import java.util.Vector;

/**
 * This is a super basic test just to make sure the test infrastructure is working correctly
 * and to act as an example of how to build template app tests.
 *
 * Created by ctsims on 8/14/2015.
 */
public class SessionStackTests {
    MockApp mApp;

    @Test
    public void testDoubleManagementAndOverlappingStack() throws Exception {
        mApp = new MockApp("/complex_stack/");
        SessionWrapper session = mApp.getSession();

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        session.setCommand("m0");

        Assert.assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());

        session.setComputedDatum();

        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        EntityDatum entityDatum = (EntityDatum)session.getNeededDatum();
        Assert.assertEquals("case_id", entityDatum.getDataId());

        Vector<Action> actions = session.getDetail(entityDatum.getShortDetail()).getCustomActions();

        if(actions == null || actions.isEmpty()) {
            Assert.fail("Detail screen stack action was missing from app!");
        }
        Action dblManagement = actions.firstElement();

        session.executeStackOperations(dblManagement.getStackOperations(), session.getEvaluationContext());

        if(session.getNeededData() != null) {
            Assert.fail("After executing stack frame steps, session should be redirected");
        }

        Assert.assertEquals("http://commcarehq.org/test/placeholder_destination", session.getForm());

        EvaluationContext ec = session.getEvaluationContext();

        CaseTestUtils.xpathEvalAndCompare(ec,"count(instance('session')/session/data/calculated_data)", 1);

        CaseTestUtils.xpathEvalAndCompare(ec,"instance('session')/session/data/calculated_data", "new");
    }

    @Test
    public void testViewNav() throws Exception {
        mApp = new MockApp("/complex_stack/");
        SessionWrapper session = mApp.getSession();

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        session.setCommand("m3-f0");

        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());

        Assert.assertEquals("case_id_to_send", session.getNeededDatum().getDataId());

        Assert.assertFalse("Session incorrectly determined a view command", session.isViewCommand(session.getCommand()));

        session.setDatum("case_id_to_send", "case_one");

        session.finishExecuteAndPop(session.getEvaluationContext());

        Assert.assertEquals("m2", session.getCommand());

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/case_id", "case_one");

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "count(instance('session')/session/data/case_id_to_send)", "0");

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
    }

    @Test
    public void testViewNonNav() throws Exception {
        mApp = new MockApp("/complex_stack/");
        SessionWrapper session = mApp.getSession();

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        session.setCommand("m4-f0");

        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());

        Assert.assertEquals("case_id_to_view", session.getNeededDatum().getDataId());

        Assert.assertTrue("Session incorrectly tagged a view command", session.isViewCommand(session.getCommand()));
    }

    @Test
    public void testOutOfOrderStack() throws Exception {
        mApp = new MockApp("/session-tests-template/");
        SessionWrapper session = mApp.getSession();

        // Select a form that has 3 datum requirements to enter (in order from suite.xml: case_id,
        // case_id_new_visit_0, usercase_id)
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0");

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0-f3");

        // Set 2 of the 3 needed datums, but not in order (1st and 3rd)
        session.setDatum("case_id", "case_id_value");
        session.setDatum("usercase_id", "usercase_id_value");

        // Session should now need the case_id_new_visit_0, which is a computed datum
        Assert.assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());

        // The key of the needed datum should be "case_id_new_visit_0"
        Assert.assertEquals("case_id_new_visit_0", session.getNeededDatum().getDataId());

        // Add the needed datum to the stack and confirm that the session is now ready to proceed
        session.setDatum("case_id_new_visit_0", "visit_id_value");
        Assert.assertEquals(null, session.getNeededData());
    }

    @Test
    public void testOutOfOrderStackComplex() throws Exception {
        mApp = new MockApp("/session-tests-template/");
        SessionWrapper session = mApp.getSession();

        // Select a form that has 3 datum requirements to enter (in order from suite.xml: case_id,
        // case_id_new_visit_0, usercase_id)
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0");

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0-f3");

        // Set 2 of the 3 needed datums, so that the datum that is actually still needed (case_id)
        // is NOT a computed value, but the "last" needed datum is a computed value
        session.setDatum("case_id_new_visit_0", "visit_id_value");
        session.setDatum("usercase_id", "usercase_id_value");

        // Session should now see that it needs a normal datum val (NOT a computed val)
        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());

        // The key of the needed datum should be "case_id"
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());

        // Add the needed datum to the stack and confirm that the session is now ready to proceed
        session.setDatum("case_id", "case_id_value");
        Assert.assertEquals(null, session.getNeededData());
    }

    @Test
    public void testUnnecessaryDataOnStack() throws Exception {
        mApp = new MockApp("/session-tests-template/");
        SessionWrapper session = mApp.getSession();

        // Select a form that has 3 datum requirements to enter (in order from suite.xml: case_id,
        // case_id_new_visit_0, usercase_id)
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0");

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0-f3");

        // Put a bunch of random data on the stack such that there are more datums on the stack
        // than the total number of needed datums for this session (which is 3)
        session.setDatum("random_id_1", "random_val_1");
        session.setDatum("random_id_2", "random_val_2");
        session.setDatum("random_id_3", "random_val_3");
        session.setDatum("random_id_4", "random_val_4");

        // Now go through and check that the session effectively ignores the rubbish on the stack
        // and still sees itself as needing each of the datums defined for this form, in the correct
        // order

        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());

        session.setDatum("case_id", "case_id_value");
        Assert.assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());
        Assert.assertEquals("case_id_new_visit_0", session.getNeededDatum().getDataId());

        session.setDatum("case_id_new_visit_0", "visit_id_value");
        Assert.assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());
        Assert.assertEquals("usercase_id", session.getNeededDatum().getDataId());

        session.setDatum("usercase_id", "usercase_id_value");
        Assert.assertEquals(null, session.getNeededData());
    }

    /**
     * Test that instances stored on the session stack (from remote query
     * results) are correctly popped off with the associated frame step
     */
    @Test
    public void testInstancesOnStack() throws Exception {
        mApp = new MockApp("/session-tests-template/");
        SessionWrapper session = mApp.getSession();

        session.setCommand("patient-search");
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_QUERY_REQUEST);

        SessionDatum datum = session.getNeededDatum();
        String bolivarsId = "123";
        TreeElement data = buildExampleInstanceRoot(bolivarsId);
        session.setQueryDatum(ExternalDataInstance.buildFromRemote(datum.getDataId(), data));

        ExprEvalUtils.testEval("instance('patients')/patients/patient/bolivar",
                session.getEvaluationContext(),
                bolivarsId);

        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());
        session.setDatum("case_id", "case_id_value");

        session.stepBack();
        ExprEvalUtils.testEval("instance('patients')/patients/patient/bolivar",
                session.getEvaluationContext(),
                bolivarsId);

        session.stepBack();
        assertInstanceMissing(session, "instance('patients')/patients/patient/bolivar");

        session.setQueryDatum(ExternalDataInstance.buildFromRemote(datum.getDataId(), data));
        ExprEvalUtils.testEval("instance('patients')/patients/patient/bolivar",
                session.getEvaluationContext(),
                bolivarsId);

        session.finishExecuteAndPop(session.getEvaluationContext());
        assertInstanceMissing(session, "instance('patients')/patients/patient/bolivar");
        ExprEvalUtils.testEval("instance('session')/session/data/case_id",
                session.getEvaluationContext(),
                bolivarsId);
    }

    protected static TreeElement buildExampleInstanceRoot(String bolivarsId) {
        TreeElement root = new TreeElement("patients");
        TreeElement data = new TreeElement("patient");
        root.addChild(data);
        TreeElement bolivar = new TreeElement("bolivar");
        bolivar.setValue(new StringData(bolivarsId));
        data.addChild(bolivar);
        data.addChild(new TreeElement("sanjay"));
        return root;
    }

    private static void assertInstanceMissing(SessionWrapper session, String xpath)
            throws XPathSyntaxException {
        try {
            ExprEvalUtils.xpathEval(session.getEvaluationContext(), xpath);
            Assert.fail("instance('patients') should not be available");
        } catch (XPathMissingInstanceException e) {
            // expected
        }
    }
}
