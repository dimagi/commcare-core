package org.commcare.util;

import org.javarosa.core.model.condition.EvaluationContext;

/**
 * Created by amstone326 on 9/25/15.
 */
public interface SessionNavigationResponder {

    void processSessionResponse(int statusCode);
    CommCareSession getSessionForNavigator();
    EvaluationContext getEvalContextForNavigator();

}
