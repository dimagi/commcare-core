package org.commcare.backend.session.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.session.StackObserver;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.test.utilities.MockApp;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class StackObserverTest {

    @Test
    public void stackObserverTest() throws Exception {
        MockApp mockApp = new MockApp("/stack-frame-copy-app/");
        SessionWrapper session = mockApp.getSession();

        session.setCommand("child-visit");
        session.setEntityDatum("mother_case_1", "nancy");

        Detail shortDetail = session.getPlatform().getDetail("case-list");
        Action action = shortDetail.getCustomActions(session.getEvaluationContext()).firstElement();

        StackObserver observer = new StackObserver();
        session.executeStackOperations(action.getStackOperations(), session.getEvaluationContext(),
                observer);

        assertEvents(observer, StackObserver.EventType.STEP_PUSHED, StackObserver.EventType.STEP_PUSHED);

        observer.reset();
        session.finishExecuteAndPop(session.getEvaluationContext(), observer);

        assertEvents(observer, StackObserver.EventType.STEPS_REWOUND, StackObserver.EventType.STEP_PUSHED);

        observer.reset();
        session.finishExecuteAndPop(session.getEvaluationContext(), observer);
        assertEvents(observer, StackObserver.EventType.FRAME_DROPPED);
    }

    private void assertEvents(StackObserver observer, StackObserver.EventType... expectedEvents) {
        Object[] actual = observer.getEvents().stream().map(StackObserver.StackEvent::getType).toArray();
        assertArrayEquals(actual, expectedEvents);
    }
}
