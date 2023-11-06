package org.commcare.util.screen;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.MenuLoader;
import org.commcare.util.LoggerInterface;
import org.javarosa.core.services.Logger;

import java.io.PrintStream;
import java.util.Arrays;

import datadog.trace.api.Trace;


/**
 * Screen to allow users to choose items from session menus.
 *
 * @author ctsims
 */
public class MenuScreen extends Screen {

    private SessionWrapper mSession;

    private MenuDisplayable[] mChoices;

    private MenuDisplayable[] mAllChoices;
    private String[] badges;

    private String mTitle;

    public String[] getBadges() {
        if (badges == null) {
            calculateBadges();
        }
        return badges;
    }

    public void setBadges(String[] badges) {
        this.badges = badges;
    }

    private void calculateBadges() {
        badges = new String[mChoices.length];
        for (int i = 0; i < mChoices.length; i++) {
            MenuDisplayable menu = mChoices[i];
            badges[i] = menu.getTextForBadge(mSession.getEvaluationContext(menu.getCommandID())).blockingGet();
        }
    }

    public boolean handleAutoMenuAdvance(SessionWrapper sessionWrapper) {
        if (mChoices.length == 1) {
            sessionWrapper.setCommand(mChoices[0].getCommandID());
            return true;
        }
        return false;
    }

    class ScreenLogger implements LoggerInterface {

        @Override
        public void logError(String message, Exception cause) {
            Logger.exception(message, cause);
        }

        @Override
        public void logError(String message) {
            Logger.log("exception", message);
        }
    }

    @Override
    public void init(SessionWrapper session) throws CommCareSessionException {
        mSession = session;
        String root = deriveMenuRoot(session);
        MenuLoader menuLoader = new MenuLoader(session.getPlatform(), session, root, new ScreenLogger(), false, false, false);
        this.mChoices = menuLoader.getMenus();
        this.mAllChoices = menuLoader.getAllMenus();
        this.mTitle = this.getBestTitle();
        Exception loadException = menuLoader.getLoadException();
        if (loadException != null) {
            throw new CommCareSessionException(menuLoader.getErrorMessage());
        }
    }

    @Override
    public String getScreenTitle() {
        return mTitle;
    }

    private String deriveMenuRoot(SessionWrapper session) {
        if (session.getCommand() == null) {
            return "root";
        } else {
            return session.getCommand();
        }
    }

    @Override
    public boolean prompt(PrintStream out) {
        for (int i = 0; i < mChoices.length; ++i) {
            MenuDisplayable d = mChoices[i];
            out.println(i + ") " + d.getDisplayText(mSession.getEvaluationContextWithAccumulatedInstances(d.getCommandID(), d.getRawText())));
        }
        return true;
    }

    @Override
    public String[] getOptions() {
        String[] ret = new String[mChoices.length];
        for (int i = 0; i < mChoices.length; ++i) {
            MenuDisplayable d = mChoices[i];
            ret[i] = d.getDisplayText(mSession.getEvaluationContextWithAccumulatedInstances(d.getCommandID(), d.getRawText()));
        }
        return ret;
    }

    @Trace
    @Override
    public boolean handleInputAndUpdateSession(CommCareSession session, String input, boolean allowAutoLaunch,
            String[] selectedValues, boolean respectRelevancy) {
        try {
            int i = Integer.parseInt(input);
            String commandId;
            MenuDisplayable[] choices = respectRelevancy ? mChoices : mAllChoices;
            MenuDisplayable menuDisplayable = choices[i];
            if (menuDisplayable instanceof Entry) {
                commandId = ((Entry)menuDisplayable).getCommandId();
            } else {
                commandId = ((Menu)choices[i]).getId();
            }
            session.setCommand(commandId);
            return true;
        } catch (NumberFormatException e) {
            //This will result in things just executing again, which is fine.
        }
        return false;
    }

    public MenuDisplayable[] getMenuDisplayables() {
        return mChoices;
    }

    public MenuDisplayable[] getAllChoices() {
        return mAllChoices;
    }

    private String getBestTitle() {
        return ScreenUtils.getAppTitle();
    }

    @Override
    public String toString() {
        return "MenuScreen['" + mTitle + "' with choices " + Arrays.toString(mChoices) + "]";
    }
}
