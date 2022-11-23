package org.commcare.backend.session.test;

import static org.junit.Assert.assertArrayEquals;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.StackObserver;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.test.utilities.MockApp;
import org.junit.Test;

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

        assertEvents(
                observer,
                StackObserver.StepEvent.class, StackObserver.EventType.PUSHED,
                StackObserver.StepEvent.class, StackObserver.EventType.PUSHED);

        observer.reset();
        session.finishExecuteAndPop(session.getEvaluationContext(), observer);

        assertEvents(
                observer,
                StackObserver.StepEvent.class, StackObserver.EventType.DROPPED,
                StackObserver.StepEvent.class, StackObserver.EventType.PUSHED);

        observer.reset();
        session.finishExecuteAndPop(session.getEvaluationContext(), observer);
        assertEvents(observer, StackObserver.FrameEvent.class, StackObserver.EventType.DROPPED);
    }

    private void assertEvents(StackObserver observer,
            Class<? extends StackObserver.StackEvent> event1, StackObserver.EventType type1
    ) {
        Object[] expected = new Object[] {new Pair<>(event1, type1)};
        assertEvents(observer, expected);
    }

    private void assertEvents(StackObserver observer,
            Class<? extends StackObserver.StackEvent> event1, StackObserver.EventType type1,
            Class<? extends StackObserver.StackEvent> event2, StackObserver.EventType type2
    ) {
        Object[] expected = new Object[] {new Pair<>(event1, type1), new Pair<>(event2, type2)};
        assertEvents(observer, expected);
    }
    private void assertEvents(StackObserver observer, Object[] expected) {
        Object[] actual = observer.getEvents().stream().map((e) -> new Pair<>(e.getClass(), e.getType())).toArray();
        assertArrayEquals(actual, expected);
    }
}
