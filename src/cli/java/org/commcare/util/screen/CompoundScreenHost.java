package org.commcare.util.screen;

import org.commcare.session.CommCareSession;

import java.io.PrintStream;

import datadog.trace.api.Trace;

/**
 * A compound screen is a controller for screens with internal state that requires navigation.
 *
 * Generally a compound screen will keep track of internal state and dispatch appropriate subscreens
 *
 * Created by ctsims on 8/20/2015.
 */
public abstract class CompoundScreenHost extends Screen {

    /**
     * NOTE: This must be non-null as soon as the compound screen init() is finished.
     *
     * @return The current subscreen to be receiving/submitting input from the CLI.
     */
    public abstract Subscreen getCurrentScreen();

    @Override
    public boolean prompt(PrintStream out) throws CommCareSessionException {
        getCurrentScreen().prompt(out);
        return true;
    }

    @Override
    public String[] getOptions(){
        return getCurrentScreen().getOptions();
    }

    @Trace
    @Override
    public boolean handleInputAndUpdateSession(CommCareSession session, String input,
            boolean allowAutoLaunch, String[] selectedValues, boolean respectRelevancy) throws CommCareSessionException {
        if (getCurrentScreen().handleInputAndUpdateHost(input, this, allowAutoLaunch, selectedValues)) {
            this.updateSession(session);
            return true;
        }
        return false;
    }

    /**
     * Once a subscreen has indicated that the compound screen is ready to update the session and
     * relinquish control, the parent screen should use this method to update the session
     * appropriately
     */
    protected abstract void updateSession(CommCareSession session);

}
