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

        // Stepping back should go back only 1 level
        session.stepBack();
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);
        Assert.assertEquals(session.getCommand(), "m0");
    }

    @Test
    public void testNeedsCaseFirst() {

    }




}
