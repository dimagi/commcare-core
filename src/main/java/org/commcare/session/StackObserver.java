package org.commcare.session;

import com.google.common.collect.ImmutableList;

import org.commcare.suite.model.StackFrameStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Observer class that accumulates events resulting from stack operations
 */
public class StackObserver {
    public enum EventType {
        FRAME_PUSHED, FRAME_DROPPED, STEP_PUSHED, STEPS_REWOUND, SMART_LINK_SET
    }
    public abstract class StackEvent {
        private final EventType type;

        StackEvent(EventType type) {
            this.type = type;
        }

        public EventType getType() {
            return type;
        }

        public abstract List<StackFrameStep> getSteps();

        @Override
        public String toString() {
            return "<StackEvent type=" + type + ">";
        }
    }

    class FrameEvent extends StackEvent {
        private final SessionFrame frame;

        FrameEvent(EventType type, SessionFrame frame) {
            super(type);
            this.frame = frame;
        }

        @Override
        public List<StackFrameStep> getSteps() {
            return frame.getSteps();
        }
    }

    class StepEvent extends StackEvent {
        private final List<StackFrameStep> steps;

        StepEvent(EventType type, StackFrameStep step) {
            this(type, ImmutableList.of(step));
        }

        StepEvent(EventType type, List<StackFrameStep> steps) {
            super(type);
            this.steps = steps;
        }

        @Override
        public List<StackFrameStep> getSteps() {
            return this.steps;
        }
    }

    class SmartLinkEvent extends StackEvent {
        private final String url;

        SmartLinkEvent(String url) {
            super(EventType.SMART_LINK_SET);
            this.url = url;
        }

        @Override
        public List<StackFrameStep> getSteps() {
            return Collections.emptyList();
        }
    }

    List<StackEvent> events = new ArrayList<>();

    /**
     * Called when a new frame is pushed onto the stack
     */
    public void framePushed(SessionFrame frame) {
        events.add(new FrameEvent(EventType.FRAME_PUSHED, frame));
    }

    /**
     * Called when a frame is removed or cleared
     */
    public void frameDropped(SessionFrame frame) {
        events.add(new FrameEvent(EventType.FRAME_DROPPED, frame));
    }

    /**
     * Step pushed onto the current frame
     */
    public void stepPushed(StackFrameStep step) {
        events.add(new StepEvent(EventType.STEP_PUSHED, step));
    }

    /**
     * Smart link set on the frame
     */
    public void smartLinkSet(String url) {
        events.add(new SmartLinkEvent(url));
    }

    /**
     * Steps were rewound
     */
    public void stepsRewound(List<StackFrameStep> steps) {
        events.add(new StepEvent(EventType.STEPS_REWOUND, steps));
    }

    public List<StackEvent> getEvents() {
        return events;
    }

    public List<StackFrameStep> getRemovedSteps() {
        List<StackFrameStep> removed = new ArrayList<>();
        for (StackEvent event : events) {
            switch (event.type) {
                case FRAME_DROPPED:
                case STEPS_REWOUND:
                    removed.addAll(event.getSteps());
                    break;
            }
        }
        return removed;
    }

    public void reset() {
        events = new ArrayList<>();
    }

}
