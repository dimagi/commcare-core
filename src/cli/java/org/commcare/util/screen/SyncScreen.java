package org.commcare.util.screen;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;

import java.io.PrintStream;

/**
 * Screen that displays user configurable entry texts and makes
 * a case query to the server with these fields.
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
