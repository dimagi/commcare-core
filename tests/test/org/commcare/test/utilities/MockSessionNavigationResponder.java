package org.commcare.test.utilities;

import org.commcare.util.CommCareSession;
import org.commcare.util.SessionNavigationResponder;
import org.commcare.util.mocks.SessionWrapper;
import org.javarosa.core.model.condition.EvaluationContext;

/**
 * A mock implementer of the SessionNavigationResponder interface, for testing purposes
 *
 * @author amstone
 */
public class MockSessionNavigationResponder implements SessionNavigationResponder {

    private SessionWrapper sessionWrapper;

    public MockSessionNavigationResponder(SessionWrapper sessionWrapper) {
        this.sessionWrapper = sessionWrapper;
    }

    @Override
    public void processSessionResponse(int resultCode) {

    }

    @Override
    public CommCareSession getSessionForNavigator() {
        return sessionWrapper;
    }

    @Override
    public EvaluationContext getEvalContextForNavigator() {
        return sessionWrapper.getEvaluationContext();
    }

}
