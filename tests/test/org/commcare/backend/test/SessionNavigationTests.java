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
    public void testBasicSessionWalk() {
        SessionWrapper session = mApp.getSession();

        // Before anything is done in the session, should need a command
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);

        session.setCommand("m0");
    }
}
