package org.commcare.util.mocks;

import org.commcare.core.session.SessionWrapper;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.util.CommCarePlatform;

/**
 * Extends a generic CommCare session to include context about the
 * current runtime environment
 *
 * @author ctsims
 */
public class CLISessionWrapper extends SessionWrapper {

    public CLISessionWrapper(CommCarePlatform platform, UserSandbox sandbox) {
        super(platform, sandbox);
    }

    @Override
    public CommCareInstanceInitializer getIIF() {
        if (initializer == null) {
            initializer = new CLIInstanceInitializer(this, mSandbox, mPlatform);
        }

        return initializer;
    }
}
