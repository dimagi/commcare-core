package org.commcare.util.screen;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;

import java.io.PrintStream;

/**
 * Screen that should trigger a sync on HQ, not yet implemented
 *
 * @author wspride
 */
public class SyncScreen extends Screen {

    SessionWrapper sessionWrapper;

    @Override
    public void init(SessionWrapper sessionWrapper) throws CommCareSessionException {
        this.sessionWrapper = sessionWrapper;
    }

    @Override
    public void prompt(PrintStream printStream) throws CommCareSessionException {

    }

    @Override
    public boolean handleInputAndUpdateSession(CommCareSession commCareSession, String s) throws CommCareSessionException {
        return false;
    }

    @Override
    public String[] getOptions() {
        return new String[0];
    }
}
