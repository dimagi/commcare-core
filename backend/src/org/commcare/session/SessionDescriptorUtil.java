package org.commcare.session;

import org.commcare.suite.model.StackFrameStep;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class SessionDescriptorUtil {
    public static void loadSessionFromDescriptor(String sessionDescriptor,
                                                 CommCareSession session) {
        String[] tokenStream = sessionDescriptor.split(" ");

        int current = 0;
        while (current < tokenStream.length) {
            String action = tokenStream[current];
            if (action.equals(SessionFrame.STATE_COMMAND_ID)) {
                session.setCommand(tokenStream[++current]);
            } else if (action.equals(SessionFrame.STATE_DATUM_VAL) ||
                    action.equals(SessionFrame.STATE_DATUM_COMPUTED) ||
                    action.equals(SessionFrame.STATE_UNKNOWN)) {
                session.setDatum(tokenStream[++current], tokenStream[++current]);
            }
            current++;
        }
    }

    /**
     * Serializes the session into a string which is unique for a
     * given path through the application, and which can be deserialzied
     * back into a live session.
     *
     * NOTE: Currently we rely on this state being semantically unique,
     * but it may change in the future. Rely on the specific format as
     * little as possible.
     */
    public static String createSessionDescriptor(CommCareSession session) {
        StringBuilder descriptor = new StringBuilder();
        for (StackFrameStep step : session.getFrame().getSteps()) {
            String type = step.getType();
            if (SessionFrame.STATE_QUERY_REQUEST.equals(type) ||
                    SessionFrame.STATE_SYNC_REQUEST.equals(type)) {
                // Skip adding remote server query/sync steps to the descriptor.
                // They are hard to replay (requires serializing query results)
                // and shouldn't be needed for incomplete forms
                continue;
            }
            descriptor.append(type).append(" ");
            if (SessionFrame.STATE_COMMAND_ID.equals(type)) {
                descriptor.append(step.getId()).append(" ");
            } else if (SessionFrame.STATE_DATUM_VAL.equals(type)
                    || SessionFrame.STATE_DATUM_COMPUTED.equals(type)) {
                descriptor.append(step.getId()).append(" ").append(step.getValue()).append(" ");
            }
        }
        return descriptor.toString().trim();
    }
}
