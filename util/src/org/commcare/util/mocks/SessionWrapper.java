package org.commcare.util.mocks;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.util.CommCarePlatform;
import org.commcare.session.CommCareSession;
import org.javarosa.core.model.condition.EvaluationContext;

/**
 * Extends a generic CommCare session to include context about the
 * current runtime environment
 *
 * @author ctsims
 */
public class SessionWrapper extends CommCareSession {
    
    UserSandbox mSandbox;
    CommCarePlatform mPlatform;
    CLIInstanceInitializer initializer;
    
    public SessionWrapper(CommCarePlatform platform, UserSandbox sandbox) {
        super(platform);
        this.mSandbox = sandbox;
        this.mPlatform = platform;
    }

    /**
     * @return The evaluation context for the current state.
     */
    public EvaluationContext getEvaluationContext() {
        return getEvaluationContext(getIIF());
    }

    /**
     * @param commandId The id of the command to evaluate against
     * @return The evaluation context relevant for the provided command id
     */
    public EvaluationContext getEvaluationContext(String commandId) {
        return getEvaluationContext(getIIF(), commandId);
    }

    public CLIInstanceInitializer getIIF() {
        if (initializer == null) {
            initializer = new CLIInstanceInitializer(this, mSandbox, mPlatform);
        }

        return initializer;
    }
    public CommCarePlatform getPlatform(){
        return this.mPlatform;
    }
    public UserSandbox getSandbox() {
        return this.mSandbox;
    }

    public void clearVolitiles() {
        initializer = null;
    }

    public void setComputedDatum() {
        setComputedDatum(getEvaluationContext());
    }
}
