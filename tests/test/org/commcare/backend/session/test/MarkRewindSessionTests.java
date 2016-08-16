package org.commcare.backend.session.test;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.test.utilities.MockApp;

import org.commcare.session.SessionFrame;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class MarkRewindSessionTests {

    /**
     * Test rewinding and set needed datum occurs correctly
     */
    @Test
    public void FrameCopyAndReturnTest() throws Exception {
        MockApp mockApp = new MockApp("/stack-frame-copy-app/");
        SessionWrapper session = mockApp.getSession();

        session.setCommand("child-visit");
        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        assertEquals("mother_case_1", session.getNeededDatum().getDataId());
        session.setDatum("mother_case_1", "nancy");

        // perform 'claim' action
        Detail shortDetail = session.getPlatform().getDetail("case-list");
        Action action = shortDetail.getCustomActions().firstElement();
        // queue up action
        session.executeStackOperations(action.getStackOperations(), session.getEvaluationContext());

        // test backing out of action
        session.stepBack();
        assertEquals("child_case_1", session.getNeededDatum().getDataId());
        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getFrame().getSteps().lastElement().getType());

        // queue up action again
        session.executeStackOperations(action.getStackOperations(), session.getEvaluationContext());

        // finish action
        session.finishExecuteAndPop(session.getEvaluationContext());

        // ensure we don't need any more data to perform the visit
        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/child_case_1", "billy");
        session.finishExecuteAndPop(session.getEvaluationContext());
        assertTrue(session.getFrame().isDead());
    }

    @Test
    public void markAndRewindInCreateTest() throws Exception {
        MockApp mockApp = new MockApp("/stack-frame-copy-app/");
        SessionWrapper session = mockApp.getSession();

        session.setCommand("create-rewind-behavior");
        assertNull(session.getNeededData());

        session.finishExecuteAndPop(session.getEvaluationContext());

        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        assertEquals("child_case_1", session.getNeededDatum().getDataId());

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/mother_case_1", "real mother");
    }

    @Test
    public void rewindInCreateWithouMarkTest() throws Exception {
        MockApp mockApp = new MockApp("/stack-frame-copy-app/");
        SessionWrapper session = mockApp.getSession();

        session.setCommand("create-rewind-without-mark");
        assertNull(session.getNeededData());

        session.finishExecuteAndPop(session.getEvaluationContext());

        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/mother_case_1", "real mother");
        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/child_case_1", "billy");
    }

    /**
     * Test that rewinding without a mark in the stack is a null op
     */
    @Test
    public void returningValuesFromFramesTest() throws Exception {
        MockApp mockApp = new MockApp("/stack-frame-copy-app/");
        SessionWrapper session = mockApp.getSession();

        // start with the registration
        session.setCommand("m0");
        assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());

        assertEquals("mother_case_1", session.getNeededDatum().getDataId());

        // manually set the needed datum instead of computing it
        session.setDatum("mother_case_1", "nancy");

        // execute the stack ops for the m0-f0 entry
        session.setCommand("m0-f0");
        session.finishExecuteAndPop(session.getEvaluationContext());

        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        assertEquals("child_case_1", session.getNeededDatum().getDataId());
    }

    /**
     * Test nested mark/rewinds
     */
    @Test
    public void nestedMarkRewindTest() throws Exception {
        MockApp mockApp = new MockApp("/stack-frame-copy-app/");
        SessionWrapper session = mockApp.getSession();

        session.setCommand("nested-mark-and-rewinds-part-i");
        session.finishExecuteAndPop(session.getEvaluationContext());
        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        session.setCommand("nested-mark-and-rewinds-part-ii");
        session.finishExecuteAndPop(session.getEvaluationContext());

        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        assertEquals("child_case_1", session.getNeededDatum().getDataId());

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/mother_case_1", "the mother case id");

    }

    @Test
    public void pushRewindToNonCurrentFrame() throws Exception {
        MockApp mockApp = new MockApp("/stack-frame-copy-app/");
        SessionWrapper session = mockApp.getSession();

        session.setCommand("push-rewind-to-non-current-id-frame");
        session.finishExecuteAndPop(session.getEvaluationContext());
        assertEquals("m0", session.getCommand());

        session.finishExecuteAndPop(session.getEvaluationContext());

        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/mother_case_1", "the mother case id");
        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/child_case_1", "billy");
    }

    @Test
    public void pushIdRewindToCurrentFrame() throws Exception {
        MockApp mockApp = new MockApp("/stack-frame-copy-app/");
        SessionWrapper session = mockApp.getSession();

        session.setCommand("push-rewind-to-current-id-frame-part-i");
        session.finishExecuteAndPop(session.getEvaluationContext());
        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        session.setCommand("push-rewind-to-current-id-frame-part-ii");
        session.finishExecuteAndPop(session.getEvaluationContext());

        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        assertEquals("child_case_1", session.getNeededDatum().getDataId());

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/mother_case_1", "the mother case id");
    }
}
