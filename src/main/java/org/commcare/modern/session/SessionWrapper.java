package org.commcare.modern.session;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.session.CommCareSession;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.condition.EvaluationContext;

import java.util.Set;

/**
 * Extends a generic CommCare session to include context about the
 * current runtime environment
 *
 * @author ctsims
 */
public class SessionWrapper extends CommCareSession implements SessionWrapperInterface {

    final protected UserSandbox mSandbox;
    final protected CommCarePlatform mPlatform;
    protected CommCareInstanceInitializer initializer;

    public SessionWrapper(CommCareSession session, CommCarePlatform platform, UserSandbox sandbox) {
        this(platform, sandbox);
        this.frame = session.getFrame();
        this.setFrameStack(session.getFrameStack());
    }
    
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

    @Override
    public EvaluationContext getRestrictedEvaluationContext(String commandId,
                                                            Set<String> instancesToInclude) {
        return getEvaluationContext(getIIF(), commandId, instancesToInclude);
    }

    /**
     * @param commandId The id of the command to evaluate against
     * @return The evaluation context relevant for the provided command id
     */
    public EvaluationContext getEvaluationContext(String commandId) {
        return getEvaluationContext(getIIF(), commandId, null);
    }

    public CommCareInstanceInitializer getIIF() {
        if (initializer == null) {
            initializer = new CommCareInstanceInitializer(this, mSandbox, mPlatform);
        }

        return initializer;
    }

    @Override
    public CommCarePlatform getPlatform(){
        return this.mPlatform;
    }
    public UserSandbox getSandbox() {
        return this.mSandbox;
    }

    public void clearVolatiles() {
        initializer = null;
    }

    public void setComputedDatum() {
        setComputedDatum(getEvaluationContext());
    }

    public String getNeededData() {
        return super.getNeededData(getEvaluationContext());
    }

    public void stepBack() {
        super.stepBack(getEvaluationContext());
    }
}
