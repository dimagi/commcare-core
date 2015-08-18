package org.commcare.util.cli;

import org.commcare.core.interfaces.UserDataInterface;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
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
    public abstract void updateSession(CommCareSession session, String input) throws CommCareSessionException;

    public String getGeneralTitle(String currentTitle, UserDataInterface sandbox, CommCarePlatform platform) {
        String title = currentTitle;
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
}
