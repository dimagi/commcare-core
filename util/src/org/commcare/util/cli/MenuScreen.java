package org.commcare.util.cli;

import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.util.mocks.SessionWrapper;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * @author ctsims
 */
public class MenuScreen extends Screen {
    private MenuDisplayable[] mChoices;
    private MockUserDataSandbox mSandbox;

    private String mTitle;

    //TODO: This is now ~entirely generic other than the wrapper, can likely be
    //moved and we can centralize its usage in the other platforms
    @Override
    public void init(CommCarePlatform platform, SessionWrapper session, MockUserDataSandbox sandbox) {
        String root = deriveMenuRoot(session);

        this.mSandbox = sandbox;

        Vector<MenuDisplayable> choices = new Vector<>();

        Hashtable<String, Entry> map = platform.getMenuMap();
        EvaluationContext ec;
        for (Suite s : platform.getInstalledSuites()) {
            for (Menu m : s.getMenus()) {
                try {
                    XPathExpression relevance = m.getMenuRelevance();
                    if (m.getMenuRelevance() != null) {
                        ec = session.getEvaluationContext(m.getId());
                        if (!XPathFuncExpr.toBoolean(relevance.eval(ec))) {
                            continue;
                        }
                    }
                    if (m.getId().equals(root)) {

                        if (mTitle == null) {
                            //TODO: Do I need args, here?
                            try {
                                mTitle = m.getName().evaluate();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        for (String command : m.getCommandIds()) {
                            XPathExpression mRelevantCondition = m.getCommandRelevance(m.indexOfCommand(command));
                            if (mRelevantCondition != null) {
                                ec = session.getEvaluationContext();
                                Object ret = mRelevantCondition.eval(ec);
                                try {
                                    if (!XPathFuncExpr.toBoolean(ret)) {
                                        continue;
                                    }
                                } catch (XPathTypeMismatchException e) {
                                    e.printStackTrace();
                                    error("relevancy condition for menu item returned non-boolean value : " + ret);

                                }
                                if (!XPathFuncExpr.toBoolean(ret)) {
                                    continue;
                                }
                            }

                            Entry e = map.get(command);
                            if (e.getXFormNamespace() == null) {
                                //If this is a "view", not an "entry"
                                //we only want to display it if all of its 
                                //datums are not already present
                                if (session.getNeededDatum(e) == null) {
                                    continue;
                                }
                            }

                            choices.add(e);
                        }
                        continue;
                    }
                    if (root.equals(m.getRoot())) {
                        //make sure we didn't already add this ID
                        boolean idExists = false;
                        for (Object o : choices) {
                            if (o instanceof Menu) {
                                if (((Menu)o).getId().equals(m.getId())) {
                                    idExists = true;
                                    break;
                                }
                            }
                        }
                        if (!idExists) {
                            choices.add(m);
                        }
                    }
                } catch (XPathSyntaxException | XPathException xpse) {
                    error(xpse);
                    return;
                }
            }
        }

        this.mChoices = new MenuDisplayable[choices.size()];
        choices.copyInto(mChoices);
        setTitle();
    }

    private void setTitle() {
        String title = this.mTitle;
        if (title == null) {
            try {
                title = Localization.get("app.display.name");
            } catch (NoLocalizedTextException e) {
                //swallow. Unimportant
                title = "CommCare";
            }
        }

        String userSuffix = mSandbox.getLoggedInUser() != null ? " | " + mSandbox.getLoggedInUser().getUsername() : "";

        this.mTitle = title + userSuffix;
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
        if (this.mTitle != null) {
            out.println(this.mTitle);
            out.println("====================");
        }
        for (int i = 0; i < mChoices.length; ++i) {
            MenuDisplayable d = mChoices[i];
            out.println(i + ")" + d.getDisplayText());
        }
    }

    @Override
    public void updateSession(CommCareSession session, String input) {
        try {
            int i = Integer.parseInt(input);
            String commandId;
            if (mChoices[i] instanceof Entry) {
                commandId = ((Entry)mChoices[i]).getCommandId();
            } else {
                commandId = ((Menu)mChoices[i]).getId();
            }
            session.setCommand(commandId);
        } catch (NumberFormatException e) {

        }
    }
}
