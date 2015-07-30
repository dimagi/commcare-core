/**
 * 
 */
package org.commcare.util.cli;

import org.commcare.util.CommCareSession;
import org.commcare.util.mocks.SessionWrapper;

import java.io.PrintStream;

/**
 * @author ctsims
 *
 */
public abstract class Screen {
    
    public abstract void init(SessionWrapper session);

    
    public abstract void prompt(PrintStream out);
    public abstract void updateSession(CommCareSession session, String input);
    
    protected void error(Exception e) {
        
    }
    
    protected void error(String message) {
        
    }

}
