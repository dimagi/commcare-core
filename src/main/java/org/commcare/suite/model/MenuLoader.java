package org.commcare.suite.model;

import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.modern.session.SessionWrapperInterface;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.LoggerInterface;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.trace.ReducingTraceReporter;
import org.javarosa.core.model.utils.InstrumentationUtils;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.analysis.InstanceNameAccumulatingAnalyzer;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

/**
 * Created by willpride on 1/3/17.
 */

public class MenuLoader {

    private Exception loadException;
    private String xPathErrorMessage;
    private MenuDisplayable[] menus;
    private String[] badges;
    private LoggerInterface loggerInterface;

    private ReducingTraceReporter traceReporter;

    public MenuLoader(CommCarePlatform platform,
                      SessionWrapperInterface sessionWrapper,
                      String menuId,
                      LoggerInterface loggerInterface, boolean shouldOutputEvalTrace) {
        this.loggerInterface = loggerInterface;
        if (shouldOutputEvalTrace) {
            this.traceReporter = new ReducingTraceReporter();
        }
        this.getMenuDisplayables(platform, sessionWrapper, menuId);
    }

    public String getErrorMessage() {
        if (loadException != null) {
            String errorMessage = loadException.getMessage();
            loggerInterface.logError(errorMessage, loadException);
            return errorMessage;
        }
        return null;
    }

    private void getMenuDisplayables(CommCarePlatform platform,
                                     SessionWrapperInterface sessionWrapper,
                                                        String menuID) {

        Vector<MenuDisplayable> items = new Vector<>();
        Vector<String> badges = new Vector<>();
        Hashtable<String, Entry> map = platform.getMenuMap();
        for (Suite s : platform.getInstalledSuites()) {
            for (Menu m : s.getMenus()) {
                try {
                    if (m.getId().equals(menuID)) {
                        if (menuIsRelevant(sessionWrapper, m)) {
                            addRelevantCommandEntries(sessionWrapper, m, items, badges, map);
                        }
                    } else {
                        addUnaddedMenu(sessionWrapper, menuID, m, items, badges);
                    }
                } catch (CommCareInstanceInitializer.FixtureInitializationException
                        | XPathSyntaxException | XPathException xpe) {
                    setLoadException(xpe);
                    menus = new MenuDisplayable[0];
                    return;
                }
            }
        }
        menus = new MenuDisplayable[items.size()];
        items.copyInto(menus);
        this.badges = new String[badges.size()];
        badges.copyInto(this.badges);
    }

    private void addUnaddedMenu(SessionWrapperInterface sessionWrapper,
                                       String menuID, Menu m,
                                       Vector<MenuDisplayable> items,
                                       Vector<String> badges) throws XPathSyntaxException {
        if (menuID.equals(m.getRoot())) {
            //make sure we didn't already add this ID
            boolean idExists = false;
            for (Object o : items) {
                if (o instanceof Menu) {
                    if (((Menu)o).getId().equals(m.getId())) {
                        idExists = true;
                        break;
                    }
                }
            }
            if (!idExists) {
                if (menuIsRelevant(sessionWrapper, m)) {
                    items.add(m);
                    badges.add(m.getTextForBadge(sessionWrapper.getEvaluationContext(m.getCommandID())).blockingGet());
                }
            }
        }
    }

    private boolean menuIsRelevant(SessionWrapperInterface sessionWrapper, Menu m) throws XPathSyntaxException {
        XPathExpression relevance = m.getMenuRelevance();
        if (m.getMenuRelevance() != null) {
            xPathErrorMessage = m.getMenuRelevanceRaw();

            Set<String> instancesNeededByRelevancyCondition =
                    (new InstanceNameAccumulatingAnalyzer()).accumulate(relevance);
            EvaluationContext ec = sessionWrapper.getRestrictedEvaluationContext(m.getId(),
                    instancesNeededByRelevancyCondition);

            EvaluationContext traceableContext = new EvaluationContext(ec, ec.getOriginalContext());
            if (traceReporter != null) {
                traceableContext.setDebugModeOn(traceReporter);
            }

            boolean result = FunctionUtils.toBoolean(relevance.eval(traceableContext));
            InstrumentationUtils.printAndClearTraces(traceReporter, "menu load expand");
            return result;
        }
        return true;
    }

    private void addRelevantCommandEntries(SessionWrapperInterface sessionWrapper,
                                           Menu m,
                                           Vector<MenuDisplayable> items,
                                           Vector<String> badges,
                                           Hashtable<String, Entry> map)
            throws XPathSyntaxException {
        xPathErrorMessage = "";
        for (String command : m.getCommandIds()) {
            XPathExpression relevancyCondition = m.getCommandRelevance(m.indexOfCommand(command));
            if (relevancyCondition != null) {
                xPathErrorMessage = m.getCommandRelevanceRaw(m.indexOfCommand(command));

                Set<String> instancesNeededByRelevancyCondition =
                        (new InstanceNameAccumulatingAnalyzer()).accumulate(relevancyCondition);
                EvaluationContext ec = sessionWrapper.getRestrictedEvaluationContext(command,
                        instancesNeededByRelevancyCondition);

                Object ret = relevancyCondition.eval(ec);
                try {
                    if (!FunctionUtils.toBoolean(ret)) {
                        continue;
                    }
                } catch (XPathTypeMismatchException e) {
                    final String msg = "relevancy condition for menu item returned non-boolean value : " + ret;
                    xPathErrorMessage = msg;
                    loadException = e;
                    loggerInterface.logError(msg, e);
                    throw e;
                }
            }

            Entry e = map.get(command);
            if (e.isView()) {
                //If this is a "view", not an "entry"
                //we only want to display it if all of its
                //datums are not already present
                if (sessionWrapper.getNeededDatum(e) == null) {
                    continue;
                }
            }

            items.add(e);
            badges.add(e.getTextForBadge(sessionWrapper.getEvaluationContext(e.getCommandId())).blockingGet());
        }
    }

    public Exception getLoadException() {
        return loadException;
    }

    public void setLoadException(Exception loadException) {
        this.loadException = loadException;
    }

    public String getxPathErrorMessage() {
        return xPathErrorMessage;
    }

    public void setxPathErrorMessage(String xPathErrorMessage) {
        this.xPathErrorMessage = xPathErrorMessage;
    }

    public MenuDisplayable[] getMenus() {
        return menus;
    }

    public void setMenus(MenuDisplayable[] menus) {
        this.menus = menus;
    }

    public String[] getBadgeText() {
        return badges;
    }

    public void setBadgeText(String[] badgeText) {
        this.badges = badgeText;
    }
}
