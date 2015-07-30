/**
 * 
 */
package org.commcare.util.mocks;

import org.commcare.api.interfaces.UserDataInterface;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
import org.javarosa.core.model.condition.EvaluationContext;

/**
 * Extends a generic CommCare session to include context about the
 * current runtime environment
 * 
 * @author ctsims
 *
 */
public class SessionWrapper extends CommCareSession {
    
    UserDataInterface mSandbox;
    CommCarePlatform mPlatform;
    
    public SessionWrapper(CommCarePlatform platform, UserDataInterface sandbox) {
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
    
    CommCareInstanceInitializer initializer;
    public CommCareInstanceInitializer getIIF() {
        if(initializer == null) {
            initializer = new CommCareInstanceInitializer(this, mSandbox, mPlatform);
        } 
        
        return initializer;
    }
}
