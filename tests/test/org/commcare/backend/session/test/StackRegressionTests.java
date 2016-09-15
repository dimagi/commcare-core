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
     * Load form title from session where the case id is computed, not
     * selected, and the case name is loaded from detail referenced by m0-f0,
     * which is implicitly referenced
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
}
