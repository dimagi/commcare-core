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

    /**
     * Ensure that session nav takes all menu entries with the same id into account
     * when determining the next needed datum
     */
    @Test
    public void testNeedsCommandFirst() throws Exception {
        MockApp app = new MockApp("/session-tests-template/");
        SessionWrapper session = app.getSession();
        session.setCommand("parent-module");

        // since there are two entries registered under 'parent-module' that
        // need different case data, we need to select the entry before the case
        assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);
        session.setCommand("adolescent-form");

        // check that after choosing the entry we now need the correct case data
        assertEquals(session.getNeededData(), SessionFrame.STATE_DATUM_VAL);
        assertEquals(session.getNeededDatum().getDataId(), "adolescent_case_id");
        session.setDatum("adolescent_case_id", "Al");
    }
}
