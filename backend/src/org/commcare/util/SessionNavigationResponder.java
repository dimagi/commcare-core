package org.commcare.util;

import org.javarosa.core.model.condition.EvaluationContext;

/**
 * Interface defining all functionality to be implemented by any class that will receive and
 * process status codes from a SessionNavigator
 *
 * @author amstone
 */
public interface SessionNavigationResponder {

    // Define responses to each of the status codes in SessionNavigator.java
    void processSessionResponse(int statusCode);

    // Provide a hook to the current CommCareSession that the SessionNavigator should be polling
    CommCareSession getSessionForNavigator();

    // Provide a hook to the current evaluation context that the SessionNavigator will use
    EvaluationContext getEvalContextForNavigator();

}
