package org.commcare.util.cli;

import org.commcare.core.interfaces.AbstractUserSandbox;
import org.commcare.util.CommCarePlatform;
import org.commcare.session.CommCareSession;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.util.mocks.SessionWrapper;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;

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

    /**
     * Based on the the input provided from the user to the command line, either update the session
     * and proceed to the next screen, or handle the input locally and ask to be redrawn
     *
     * @param session The current session to be mutated
     * @param input the input provided by the user to the command line
     * @return True if the session was updated and the app should proceed to the next phase, false
     * if the screen wants to continue being redrawn.
     */
    public abstract boolean handleInputAndUpdateSession(CommCareSession session, String input) throws CommCareSessionException;


    /**
     * Get the title of the current screen, wrapped with the relevant app metadata.
     */
    public String getWrappedDisplaytitle(AbstractUserSandbox sandbox, CommCarePlatform platform) {
        String title = getScreenTitle();
        if (title == null) {
            try {
                title = Localization.get("app.display.name");
            } catch (NoLocalizedTextException e) {
                //swallow. Unimportant
                title = "CommCare";
            }
        }

        String userSuffix = sandbox.getLoggedInUser() != null ? " | " + sandbox.getLoggedInUser().getUsername() : "";
        String version = (" [" + platform.getCurrentProfile().getVersion() + "]");

        return title + userSuffix + version;
    }

    /**
     * @return The title of this specific screen. This method should be overriden by subclasses
     * who can provide a title.
     */
    protected String getScreenTitle() {
        return null;
    }
}
