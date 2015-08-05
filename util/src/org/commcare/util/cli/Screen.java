package org.commcare.util.cli;

import org.commcare.util.CommCareSession;
import org.commcare.util.mocks.SessionWrapper;

import java.io.PrintStream;

/**
 * A screen is a user interaction component which provides the session state needed to display
 * a choice to the user, and should be capable of updating the session appropriately based on
 * the user's decision.
 *
 * @author ctsims
 */
public abstract class Screen {
    public abstract void init(SessionWrapper session) throws CommCareSessionException;
    public abstract void prompt(PrintStream out) throws CommCareSessionException;
    public abstract void updateSession(CommCareSession session, String input) throws CommCareSessionException;
}
