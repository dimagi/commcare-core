package org.commcare.util.cli;

import org.commcare.core.session.SessionWrapper;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCarePlatform;
import org.commcare.session.CommCareSession;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Screen to allow users to choose items from session menus.
 *
 * @author ctsims
 */
public class MenuScreen extends Screen {
    private MenuDisplayable[] mChoices;
    CommCarePlatform mPlatform;
    UserSandbox mSandbox;
    
    String mTitle;
    
    //TODO: This is now ~entirely generic other than the wrapper, can likely be
    //moved and we can centralize its usage in the other platforms
    @Override
    public void init(SessionWrapper session) throws CommCareSessionException{
        
        String root = deriveMenuRoot(session);
        
        this.mPlatform = session.getPlatform();
        this.mSandbox = session.getSandbox();
        
        Vector<MenuDisplayable> choices = new Vector<MenuDisplayable>();
        
        Hashtable<String, Entry> map = mPlatform.getMenuMap();
        EvaluationContext ec = null;
        for(Suite s : mPlatform.getInstalledSuites()) {
            for(Menu m : s.getMenus()) {
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
                                    throw new CommCareSessionException("relevancy condition for menu item returned non-boolean value : " + ret, e);

                                }
                            }

                            Entry e = map.get(command);
                            if (e.isView()) {
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
                } catch(XPathSyntaxException xpse) {
                    throw new CommCareSessionException("Invalid XPath Expression in Text entry or module condition", xpse);
                } catch(XPathException xpe) {
                    throw new CommCareSessionException("Error evaluating expression in Text or module condition",xpe);
                }
            }
        }

        this.mChoices = new MenuDisplayable[choices.size()];
        choices.copyInto(mChoices);
    }

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
            if (mChoices[i] instanceof FormEntry) {
                commandId = ((FormEntry)mChoices[i]).getCommandId();
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
