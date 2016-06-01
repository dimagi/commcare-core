/**
 *
 */
package org.commcare.util;

import org.commcare.applogic.CommCareFormEntryState;
import org.commcare.applogic.CommCareHomeState;
import org.commcare.applogic.CommCareSelectState;
import org.commcare.applogic.MenuHomeState;
import org.commcare.entity.CommCareEntity;
import org.commcare.entity.NodeEntitySet;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.AssertionSet;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.StackOperation;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.entity.model.Entity;
import org.javarosa.j2me.util.CommCareHandledExceptionState;
import org.javarosa.j2me.util.media.ImageUtils;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.j2me.view.ProgressIndicator;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.lang.RuntimeException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import de.enough.polish.ui.List;

/**
 * The CommCare Session Controller is responsible for managing the
 * user experience of a CommCare session, from start until either
 * the session is canceled or the user arrives at a form entry
 * stage.
 *
 * @author ctsims
 *
 */
public class CommCareSessionController {

    protected CommCareSession session;

    //I hate this...
    private Hashtable<Integer, Suite> suiteTable = new Hashtable<Integer,Suite>();
    private Hashtable<Integer, Entry> entryTable = new Hashtable<Integer,Entry>();
    private Hashtable<Integer, Menu> menuTable = new Hashtable<Integer,Menu>();

    public CommCareSessionController(CommCareSession session) {
        this.session = session;
    }

    public void populateMenu(List list, String menu) {
        populateMenu(list, menu, null);
    }

    public void populateMenu(List list, String menu, MultimediaListener listener) {
        suiteTable.clear();
        entryTable.clear();
        menuTable.clear();
        Enumeration en = session.getPlatform().getInstalledSuites().elements();
        while(en.hasMoreElements()) {
            Suite suite = (Suite)en.nextElement();
            for(Menu m : suite.getMenus()) {
                //don't do this lazily for now because it changes per module
                EvaluationContext ec  = null;
                try {
                    XPathExpression relevance = m.getMenuRelevance();
                    if (relevance != null) {
                        if (ec == null) {
                            ec = session.getEvaluationContext(getIif(), m.getId());
                        }
                        if (!XPathFuncExpr.toBoolean(relevance.eval(ec)).booleanValue()) {
                            continue;
                        }
                    }
                } catch (XPathSyntaxException e) {
                    e.printStackTrace();
                }
                if (menu.equals(m.getId())){
                    for(int i = 0; i < m.getCommandIds().size(); ++i) {
                        try {
                            String id = m.getCommandIds().elementAt(i);
                            XPathExpression relevant = m.getCommandRelevance(i);
                            if (relevant != null) {
                                if (ec == null) {
                                    ec = session.getEvaluationContext(getIif());
                                }
                                if (XPathFuncExpr.toBoolean(relevant.eval(ec)).booleanValue() == false) {
                                    continue;
                                }
                            }
                            Entry e = suite.getEntries().get(id);
                            if (e.isView()) {
                                //If this is a "view", not an "entry"
                                //we only want to display it if all of its
                                //datums are not already present
                                if (session.getNeededDatum(e) == null) {
                                    continue;
                                }
                            }

                            int location = list.size();
                            list.append(CommCareUtil.getMenuText(e.getText(),suite,location), ImageUtils.getImage(e.getImageURI()));
                            //TODO: All these multiple checks are pretty sloppy
                            if (listener != null && (e.getAudioURI() != null && !"".equals(e.getAudioURI()))) {
                                listener.registerAudioTrigger(location, e.getAudioURI());
                            }
                            suiteTable.put(new Integer(location),suite);
                            entryTable.put(new Integer(location),e);
                        } catch(XPathSyntaxException xpse) {
                            throw new RuntimeException(xpse.getMessage());
                        }
                    }

                }
                else if (m.getRoot().equals(menu)) {
                    int location = list.size();
                    list.append(CommCareUtil.getMenuText(m.getName(), suite,location),  ImageUtils.getImage(m.getImageURI()));
                    //TODO: All these multiple checks are pretty sloppy
                    if (listener != null && (m.getAudioURI() != null && !"".equals(m.getAudioURI()))) {
                        listener.registerAudioTrigger(location, m.getAudioURI());
                    }
                    suiteTable.put(new Integer(location),suite);
                    menuTable.put(new Integer(location),m);
                }
            }
        }
    }

    public Suite getSelectedSuite(int selectedItem) {
        Integer selected = new Integer(selectedItem);

        if (suiteTable.containsKey(selected)) {
            return suiteTable.get(selected);
        } else {
            return null;
        }
    }

    public Menu getSelectedMenu(int selectedItem) {
        Integer selected = new Integer(selectedItem);

        if (menuTable.containsKey(selected)) {
            return menuTable.get(selected);
        } else {
            return null;
        }
    }

    public Entry getSelectedEntry(int selectedItem) {
        Integer selected = new Integer(selectedItem);

        if (entryTable.containsKey(selected)) {
            return entryTable.get(selected);
        } else {
            return null;
        }
    }

    public void chooseSessionItem(int item) {
        Menu m = getSelectedMenu(item);
        if (m == null) {
            Entry e = getSelectedEntry(item);
            session.setCommand(e.getCommandId());
        } else {
            //We selected a menu, tell the session the command.
            session.setCommand(m.getId());
        }
    }

    public void next() {
        String next = session.getNeededData(session.getEvaluationContext(getIif()));
        if (next == null) {
            String xmlns = session.getForm();

            if (xmlns == null) {
                //This command is a view, not an entry. We can comfortably return to the previous step.
                this.back();
                return;
            }
            //create form entry session
            Entry entry = session.getCurrentEntry();

            if (failedAssertion(entry.getAssertions())) {
                return;
            }

            String title;
            if (CommCareSense.sense()) {
                title = null;
            } else {
                title = Localizer.clearArguments(entry.getText().evaluate());
            }

            //Start form entry and clear anything we've been using from memory
            initializer = null;

            suiteTable.clear();
            entryTable.clear();
            menuTable.clear();

            //load up any ops that we may need after the entry is over
            final Vector<StackOperation> ops = entry.getPostEntrySessionOperations();

            CommCareFormEntryState state = new CommCareFormEntryState(title,xmlns, getPreloaders(), CommCareContext._().getFuncHandlers(), getIif()) {
                protected void goHome() {
                    //Clear out any local caching, since transactions may have occurred since it was cached.
                    initializer = null;

                    //Ok, now we just need to figure out if it's time to go home, or time to fire up a new session from the stack
                    if (session.finishExecuteAndPop(session.getEvaluationContext(getIif()))) {
                        next();
                    } else {
                        J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
                    }
                }
                public void abort () {
                     back();
                }
            };
            J2MEDisplay.startStateWithLoadingScreen(state);
            return;
        }

        if (next.equals(SessionFrame.STATE_COMMAND_ID)) {
            //You only get commands from menus, so the current
            //command has to be a menu, we should load a menu state

            if (session.getCommand() == null) {
                //We're at the root selection, we need to go home
                session.clearAllState();
                J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
                return;
            }

            MenuHomeState state = new MenuHomeState(this, session.getMenu(session.getCommand())) {

                public void entry(Suite suite, Entry entry) {
                    //This shouldn't happen anymore
                    throw new RuntimeException("Clayton messed up");
                }

                public void exitMenuTransition() {
                    CommCareSessionController.this.back();
                }

            };
            J2MEDisplay.startStateWithLoadingScreen(state);
            return;
        }

        //The rest of the selections all depend on the suite being available for checkin'
        //TODO: Stuff can be in more than one suite!!!
        Suite suite = session.getCurrentSuite();

        EntityDatum datum = (EntityDatum)session.getNeededDatum();
        EvaluationContext context = session.getEvaluationContext(getIif());

        //TODO: This should be part of the next/back protocol in the session, not here.
        if (next.equals(SessionFrame.STATE_DATUM_COMPUTED)) {
            try {
                session.setComputedDatum(context);
            } catch (XPathException e) {
                throw new RuntimeException(e.getMessage());
            }
            next();
            return;
        }


        Detail shortDetail = suite.getDetail(datum.getShortDetail());
        Detail longDetail = null;
        if (datum.getLongDetail() != null) {
            longDetail = suite.getDetail(datum.getLongDetail());
        }

        final NodeEntitySet nes = new NodeEntitySet(datum.getNodeset(), context);
        Entity<TreeReference> entity = new CommCareEntity(shortDetail, longDetail, context, nes);

        final CommCareSelectState<TreeReference> select = new CommCareSelectState<TreeReference>(entity, nes) {
            SessionDatum datum;
            EvaluationContext context;

            {
                 datum = session.getNeededDatum();
                 context = session.getEvaluationContext(getIif());
            }

            public void cancel() {
                CommCareSessionController.this.back();
            }

            public void entitySelected(int id) {
                TreeReference selected = nes.get(id);
                TreeReference outcome = XPathReference.getPathExpr(datum.getValue()).getReference().contextualize(selected);
                AbstractTreeElement element = context.resolveReference(outcome);
                if (element == null) {
                    throw new RuntimeException("No reference resolved for: " + outcome.toString());
                }
                String outputData = element.getValue().uncast().getString();
                CommCareSessionController.this.session.setDatum(datum.getDataId(), outputData);
                CommCareSessionController.this.next();
            }
        };

        J2MEDisplay.startStateWithLoadingScreen(select, new ProgressIndicator() {
            public double getProgress() {
                if (nes.loaded()) {
                    return select.getProgressIndicator().getProgress();
                } else {
                    return nes.getProgress();
                }
            }

            public String getCurrentLoadingStatus() {
                if (nes.loaded()) {
                    return select.getProgressIndicator().getCurrentLoadingStatus();
                } else {
                    return nes.getCurrentLoadingStatus();
                }
            }

            public int getIndicatorsProvided() {
                if (nes.loaded()) {
                    return select.getProgressIndicator().getIndicatorsProvided();
                } else {
                    return nes.getIndicatorsProvided();
                }
            }

        });
        return;
    }

    private boolean failedAssertion(AssertionSet assertionSet) {
        //Check to see whether there are any issues here:
        EvaluationContext ec = session.getEvaluationContext(getIif());
        Text assertionFailure = assertionSet.getAssertionFailure(ec);
        if (assertionFailure != null) {
            final String failureMsg = assertionFailure.evaluate(ec);
            CommCareHandledExceptionState assertFailState = new CommCareHandledExceptionState() {

                public String getExplanationMessage(String e) {
                    return failureMsg;
                }

                public boolean handlesException(Exception e) {
                    return true;
                }

                public void done() {
                    CommCareSessionController.this.back();
                }
            };
            J2MEDisplay.startStateWithLoadingScreen(assertFailState);
            return true;
        }
        return false;
    }

    CommCareInstanceInitializer initializer = null;

    private InstanceInitializationFactory getIif() {
        if (initializer == null) {
            initializer = new CommCareInstanceInitializer(CommCareStatic.appStringCache, this.session);
        }
        return initializer;
    }

    private Vector<IPreloadHandler> getPreloaders() {
        return CommCareContext._().getPreloaders();
    }

    protected void back() {
        session.stepBack(session.getEvaluationContext(getIif()));
        next();
    }
}
