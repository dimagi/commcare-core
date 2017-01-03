package org.commcare.util.screen;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.MenuLoader;

import java.io.PrintStream;

/**
 * Screen to allow users to choose items from session menus.
 *
 * @author ctsims
 */
public class MenuScreen extends Screen {

    private MenuDisplayable[] mChoices;

    String mTitle;

    @Override
    public void init(SessionWrapper session) throws CommCareSessionException {
        String root = deriveMenuRoot(session);
        MenuLoader menuLoader = new MenuLoader(session.getPlatform(), session, root);
        this.mChoices = menuLoader.getMenus();

        Exception loadException = menuLoader.getLoadException();

        if (loadException != null) {
            throw new CommCareSessionException(menuLoader.getErrorMessage());
        }

    }

    @Override
    protected String getScreenTitle() {
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
    public void prompt(PrintStream out) {
        for (int i = 0; i < mChoices.length; ++i) {
            MenuDisplayable d = mChoices[i];
            out.println(i + ")" + d.getDisplayText());
        }
    }

    @Override
    public String[] getOptions() {
        String[] ret = new String[mChoices.length];
        for (int i = 0; i < mChoices.length; ++i) {
            MenuDisplayable d = mChoices[i];
            ret[i] = d.getDisplayText();
        }
        return ret;
    }

    @Override
    public boolean handleInputAndUpdateSession(CommCareSession session, String input) {
        try {
            int i = Integer.parseInt(input);
            String commandId;
            MenuDisplayable menuDisplayable = mChoices[i];
            if (menuDisplayable instanceof Entry) {
                commandId = ((Entry)menuDisplayable).getCommandId();
            } else {
                commandId = ((Menu)mChoices[i]).getId();
            }
            session.setCommand(commandId);
            return false;
        } catch (NumberFormatException e) {
            //This will result in things just executing again, which is fine.
        }
        return true;
    }
}
