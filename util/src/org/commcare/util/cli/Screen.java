/**
 * 
 */
package org.commcare.util.cli;

import org.commcare.api.interfaces.UserDataInterface;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
import org.commcare.util.mocks.SessionWrapper;

import java.io.PrintStream;

/**
 * @author ctsims
 *
 */
public abstract class Screen {
    
    public abstract void init(CommCarePlatform platform, SessionWrapper session, UserDataInterface sandbox);

    
    public abstract void prompt(PrintStream out);
    public abstract void updateSession(CommCareSession session, String input);
    
    protected void error(Exception e) {
        
    }
    
    protected void error(String message) {
        
    }

}
