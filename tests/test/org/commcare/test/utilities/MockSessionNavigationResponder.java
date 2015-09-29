package org.commcare.test.utilities;

import org.commcare.session.CommCareSession;
import org.commcare.session.SessionNavigationResponder;
import org.commcare.util.mocks.SessionWrapper;
import org.javarosa.core.model.condition.EvaluationContext;

/**
 * A mock implementer of the SessionNavigationResponder interface, for testing purposes
 *
 * @author amstone
 */
public class MockSessionNavigationResponder implements SessionNavigationResponder {

    private SessionWrapper sessionWrapper;
    private int lastReceivedResultCode;

    public MockSessionNavigationResponder(SessionWrapper sessionWrapper) {
        this.sessionWrapper = sessionWrapper;
    }

    public int getLastResultCode() {
        return this.lastReceivedResultCode;
    }

    @Override
    public void processSessionResponse(int resultCode) {
        lastReceivedResultCode = resultCode;
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
