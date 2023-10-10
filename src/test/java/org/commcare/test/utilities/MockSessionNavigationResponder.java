package org.commcare.test.utilities;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionNavigationResponder;
import org.javarosa.core.model.condition.EvaluationContext;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A mock implementer of the SessionNavigationResponder interface, for testing purposes
 *
 * @author amstone
 */
public class MockSessionNavigationResponder implements SessionNavigationResponder {

    private final SessionWrapper sessionWrapper;
    private int lastReceivedResultCode;
    private ReentrantLock backgroungSyncLock;

    public MockSessionNavigationResponder(SessionWrapper sessionWrapper) {
        this.sessionWrapper = sessionWrapper;
        this.backgroungSyncLock = new ReentrantLock();
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

    @Override
    public ReentrantLock getBackgroundSyncLock() {
        return backgroungSyncLock;
    }

}
