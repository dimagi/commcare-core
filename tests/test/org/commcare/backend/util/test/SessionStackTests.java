package org.commcare.backend.util.test;

import org.commcare.suite.model.Action;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.test.utilities.MockApp;
import org.commcare.util.mocks.SessionWrapper;
import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.Assert;

import org.commcare.session.SessionFrame;
import org.junit.Before;
import org.junit.Test;

/**
 * This is a super basic test just to make sure the test infrastructure is working correctly
 * and to act as an example of how to build template app tests.
 *
 * Created by ctsims on 8/14/2015.
 */
public class SessionStackTests {
    MockApp mApp;

    @Before
    public void init() throws Exception{
        mApp = new MockApp("/complex_stack/");
    }

    @Test
    public void testDoubleManagementAndOverlappingStack() throws Exception {
        SessionWrapper session = mApp.getSession();
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);

        session.setCommand("m0");

        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_DATUM_COMPUTED);

        session.setComputedDatum();

        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_DATUM_VAL);
        Assert.assertEquals(session.getNeededDatum().getDataId(), "case_id");

        Action dblManagement = session.getDetail(session.getNeededDatum().getShortDetail()).getCustomAction();

        if(dblManagement == null) {
            Assert.fail("Detail screen stack action was missing from app!");
        }

        session.executeStackOperations(dblManagement.getStackOperations(), session.getEvaluationContext());

        if(session.getNeededData() != null) {
            Assert.fail("After executing stack frame steps, session should be redirected");
        }

        Assert.assertEquals(session.getForm(), "http://commcarehq.org/test/placeholder_destination");

        EvaluationContext ec = session.getEvaluationContext();

        CaseTestUtils.xpathEvalAndCompare(ec,"count(instance('session')/session/data/calculated_data)", 1);

        CaseTestUtils.xpathEvalAndCompare(ec,"instance('session')/session/data/calculated_data", "new");
    }

    @Test
    public void testViewNav() throws Exception {
        SessionWrapper session = mApp.getSession();
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);

        session.setCommand("m3-f0");

        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_DATUM_VAL);

        Assert.assertEquals(session.getNeededDatum().getDataId(), "case_id_to_send");

        Assert.assertFalse("Session incorrectly determined a view command", session.isViewCommand(session.getCommand()));

        session.setDatum("case_id_to_send", "case_one");

        session.finishExecuteAndPop(session.getEvaluationContext());

        Assert.assertEquals(session.getCommand(), "m2");

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/case_id", "case_one");

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "count(instance('session')/session/data/case_id_to_send)", "0");

        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);
    }

    @Test
    public void testViewNonNav() throws Exception {
        SessionWrapper session = mApp.getSession();
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);

        session.setCommand("m4-f0");

        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_DATUM_VAL);

        Assert.assertEquals(session.getNeededDatum().getDataId(), "case_id_to_view");

        Assert.assertTrue("Session incorrectly tagged a view command", session.isViewCommand(session.getCommand()));
    }

}
