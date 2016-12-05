package org.commcare.backend.session.test;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionDescriptorUtil;
import org.commcare.session.SessionFrame;
import org.commcare.test.utilities.MockApp;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by willpride on 9/15/16.
 */

public class StackRegressionTests {
    /**
     * This is the stack state we would be in after end of form navigation 
     * - this test ensures that we correctly resolve the unknown state (a 
     * case selection)
     */
    @Test
    public void testGoBackAfterEndOfFormNavigation() throws Exception {
        MockApp mockApp = new MockApp("/nav_back/");
        SessionWrapper session = mockApp.getSession();
        UserSandbox sandbox = session.getSandbox();
        SessionWrapper blankSession = new SessionWrapper(session.getPlatform(), sandbox);
        String descriptor = "COMMAND_ID m1 " +
                "STATE_UNKNOWN case_id test_id";
        SessionDescriptorUtil.loadSessionFromDescriptor(descriptor, blankSession);
        assertEquals(SessionFrame.STATE_COMMAND_ID, blankSession.getNeededData());
        blankSession.stepBack();
        assertEquals(SessionFrame.STATE_DATUM_VAL, blankSession.getNeededData());
        blankSession.stepBack();
        assertEquals(SessionFrame.STATE_COMMAND_ID, blankSession.getNeededData());
    }

    @Test
    public void testKeepComputedDatum() throws Exception {
        MockApp mockApp = new MockApp("/nav_back/");
        SessionWrapper session = mockApp.getSession();
        UserSandbox sandbox = session.getSandbox();
        SessionWrapper blankSession = new SessionWrapper(session.getPlatform(), sandbox);
        String descriptor = "COMMAND_ID m0-f0 " +
                "COMPUTED_DATUM case_id_new_mother_0 test_id " +
                "COMPUTED_DATUM return_to m1";
        SessionDescriptorUtil.loadSessionFromDescriptor(descriptor, blankSession);
        assertEquals(2, blankSession.getData().size());
    }
}
