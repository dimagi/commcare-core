package org.commcare.util.mocks;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.util.CommCarePlatform;

/**
 * Created by willpride on 10/22/15.
 *
 * Override default initializer so we can add our own version string
 */
public class CLIInstanceInitializer extends CommCareInstanceInitializer {

    public CLIInstanceInitializer(UserSandbox sandbox){
        super(sandbox);
    }

    public CLIInstanceInitializer(CLISessionWrapper sessionWrapper, UserSandbox mSandbox, CommCarePlatform mPlatform) {
        super(sessionWrapper, mSandbox, mPlatform);
    }

    public String getVersionString(){
        return "CommCare CLI Version: " + mPlatform.getMajorVersion() + "." + mPlatform.getMinorVersion();
    }
}
