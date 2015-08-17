package org.commcare.backend.test;

import org.commcare.test.utilities.MockApp;
import org.commcare.util.SessionFrame;
import org.commcare.util.mocks.SessionWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by amstone326 on 8/17/15.
 */
public class SessionNavigationTests {

    MockApp mApp;

    @Before
    public void setUp() throws Exception {
        mApp = new MockApp("/session-tests-template/");
    }

    @Test
    public void testNeedsCommandFirst() {
        SessionWrapper session = mApp.getSession();

        // Before anything is done in the session, should need a command
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);

        // After setting first command to m0, should still need another command because the 3 forms
        // within m0 have different datum needs, so will prioritize getting the next command first
        session.setCommand("m0");
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);

        // After choosing the form, will need a case id
        session.setCommand("m0-f1");
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_DATUM_VAL);
        Assert.assertEquals(session.getNeededDatum().getDataId(), "case_id");

        // Stepping back after choosing a command should go back only 1 level
        session.stepBack();
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);
        Assert.assertEquals(session.getCommand(), "m0");

        // After choosing registration form, should need computed case id
        session.setCommand("m0-f0");
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_DATUM_COMPUTED);

        //TODO: Figure out how to run a datum compute from here
    }

    @Test
    public void testNeedsCaseFirst() {
        SessionWrapper session = mApp.getSession();

        // Before anything is done in the session, should need a command
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);

        // After setting first command to m2, should need a case id, because both forms within m2 need one
        session.setCommand("m2");
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_DATUM_VAL);
        Assert.assertEquals(session.getNeededDatum().getDataId(), "case_id");

        // After setting case id, should need to choose a form
        session.setDatum("case_id", "case_two");
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);

        // Should be ready to go after choosing a form
        session.setCommand("m2-f1");
        Assert.assertEquals(session.getNeededData(), null);
    }

    @Test
    public void testStepBack() {
        //TODO: test stepBack() with many different initial configurations of the stack
    }

}
