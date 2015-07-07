/**
 * 
 */
package org.commcare.util.cli;

import java.io.PrintStream;

import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.util.mocks.SessionWrapper;

/**
 * @author ctsims
 *
 */
public abstract class Screen {
    
    public abstract void init(CommCarePlatform platform, SessionWrapper session, MockUserDataSandbox sandbox);

    
    public abstract void prompt(PrintStream out);
    public abstract void updateSession(CommCareSession session, String input);
    
    protected void error(Exception e) {
        
    }
    
    protected void error(String message) {
        
    }

}
