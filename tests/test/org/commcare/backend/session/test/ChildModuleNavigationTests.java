package org.commcare.backend.session.test;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.test.utilities.MockApp;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */

public class ChildModuleNavigationTests {

    @Test
    public void testNeedsCommandFirst() throws Exception {
        MockApp app = new MockApp("/session-tests-template/");
        SessionWrapper session = app.getSession();
        session.setCommand("parent-module");
        assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);
        session.setCommand("adolescent-form");
        assertEquals(session.getNeededData(), SessionFrame.STATE_DATUM_VAL);
        assertEquals(session.getNeededDatum().getDataId(), "adolescent_case_id");
        session.setDatum("adolescent_case_id", "Al");
    }
}
