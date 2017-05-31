package org.commcare.session;

import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.XPathException;

import java.util.Vector;

/**
 * Performs all logic involved in polling the current CommCareSession to determine what information
 * or action is needed by the session, and then sends a signal to the registered
 * SessionNavigationResponder to indicate what should be done to get it (or if an error occurred)
 *
 * @author amstone
 */
public class SessionNavigator {

    // Result codes to be interpreted by the SessionNavigationResponder
    public static final int ASSERTION_FAILURE = 0;
    public static final int NO_CURRENT_FORM = 1;
    public static final int START_FORM_ENTRY = 2;
    public static final int GET_COMMAND = 3;
    public static final int START_ENTITY_SELECTION = 4;
    public static final int LAUNCH_CONFIRM_DETAIL = 5;
    public static final int XPATH_EXCEPTION_THROWN = 6;
    public static final int START_SYNC_REQUEST = 7;
    public static final int PROCESS_QUERY_REQUEST = 8;
    public static final int REPORT_CASE_AUTOSELECT = 9;

    private final SessionNavigationResponder responder;
    private CommCareSession currentSession;
    private EvaluationContext ec;

    private TreeReference currentAutoSelectedCase;
    private XPathException thrownException;


    public SessionNavigator(SessionNavigationResponder r) {
        this.responder = r;
    }

    private void sendResponse(int resultCode) {
        responder.processSessionResponse(resultCode);
    }

    public TreeReference getCurrentAutoSelection() {
        return this.currentAutoSelectedCase;
    }

    public XPathException getCurrentException() {
        return this.thrownException;
    }

    /**
     * Polls the CommCareSession to determine what information is needed in order to proceed with
     * the next entry step in the session, and then executes the action to get that info, OR
     * proceeds with trying to enter the form if no more info is needed
     */
    public void startNextSessionStep() {
        currentSession = responder.getSessionForNavigator();
        ec = responder.getEvalContextForNavigator();
        String needed;
        try {
            needed = currentSession.getNeededData(ec);
        } catch (XPathException e) {
            thrownException = e;
            sendResponse(XPATH_EXCEPTION_THROWN);
            return;
        }

        dispatchOnNeededData(needed);
    }

    private void dispatchOnNeededData(String needed) {
        if (needed == null) {
            readyToProceed();
            return;
        }

        switch (needed) {
            case SessionFrame.STATE_COMMAND_ID:
                sendResponse(GET_COMMAND);
                break;
            case SessionFrame.STATE_SYNC_REQUEST:
                sendResponse(START_SYNC_REQUEST);
                break;
            case SessionFrame.STATE_QUERY_REQUEST:
                sendResponse(PROCESS_QUERY_REQUEST);
                break;
            case SessionFrame.STATE_DATUM_VAL:
                handleGetDatum();
                break;
            case SessionFrame.STATE_DATUM_COMPUTED:
                handleCompute();
                break;
        }
    }

    private void readyToProceed() {
        Text text = currentSession.getCurrentEntry().getAssertions().getAssertionFailure(ec);
        if (text != null) {
            // We failed one of our assertions
            sendResponse(ASSERTION_FAILURE);
        }
        else if (currentSession.getForm() == null) {
            sendResponse(NO_CURRENT_FORM);
        } else {
            sendResponse(START_FORM_ENTRY);
        }
    }

    private void handleGetDatum() {
        TreeReference autoSelection = getAutoSelectedCase();
        if (autoSelection == null) {
            sendResponse(START_ENTITY_SELECTION);
        } else {
            sendResponse(REPORT_CASE_AUTOSELECT);
            this.currentAutoSelectedCase = autoSelection;
            handleAutoSelect();
        }
    }

    private void handleAutoSelect() {
        SessionDatum selectDatum = currentSession.getNeededDatum();
        if (selectDatum instanceof EntityDatum) {
            EntityDatum entityDatum = (EntityDatum)selectDatum;
            if (entityDatum.getLongDetail() == null) {
                // No confirm detail defined for this entity select, so just set the case id right away
                // and proceed
                String autoSelectedCaseId = EntityDatum.getCaseIdFromReference(
                        currentAutoSelectedCase, entityDatum, ec);
                currentSession.setDatum(entityDatum.getDataId(), autoSelectedCaseId);
                startNextSessionStep();
            } else {
                sendResponse(LAUNCH_CONFIRM_DETAIL);
            }
        }
    }

    /**
     * Returns the auto-selected case for the next needed datum, if there should be one.
     * Returns null if auto selection is not enabled, or if there are multiple available cases
     * for the datum (and therefore auto-selection should not be used).
     */
    private TreeReference getAutoSelectedCase() {
        SessionDatum selectDatum = currentSession.getNeededDatum();
        if (selectDatum instanceof EntityDatum) {
            return ((EntityDatum)selectDatum).getCurrentAutoselectableCase(this.ec);
        }
        return null;
    }

    private void handleCompute() {
        try {
            currentSession.setComputedDatum(ec);
        } catch (XPathException e) {
            this.thrownException = e;
            sendResponse(XPATH_EXCEPTION_THROWN);
            return;
        }
        startNextSessionStep();
    }

    public void stepBack() {
        currentSession.stepBack(ec);
    }
}
