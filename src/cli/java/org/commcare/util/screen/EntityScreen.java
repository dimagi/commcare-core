package org.commcare.util.screen;

import org.commcare.cases.entity.EntityUtil;
import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.queryset.CurrentModelQuerySet;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Endpoint;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.DatumUtil;
import org.commcare.util.FormDataUtil;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.EvaluationTraceReporter;
import org.javarosa.core.model.utils.InstrumentationUtils;
import org.javarosa.core.util.NoLocalizedTextException;

import java.util.Hashtable;
import java.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import datadog.trace.api.Trace;

/**
 * Compound Screen to select an entity from a list and then display the one or more details that
 * are associated with the entity.
 *
 * Does not currently support tile based selects
 *
 * @author ctsims
 */
public class EntityScreen extends CompoundScreenHost {

    private final EntityScreenContext entityScreenContext;
    private TreeReference mCurrentSelection;

    private SessionWrapper mSession;
    private CommCarePlatform mPlatform;

    private Detail mShortDetail;

    protected EntityDatum mNeededDatum;
    private Action mPendingAction;

    private Subscreen<EntityScreen> mCurrentScreen;

    private boolean readyToSkip = false;
    private EvaluationContext evalContext;

    protected Hashtable<String, TreeReference> referenceMap;

    private boolean handleCaseIndex;
    private boolean needsFullInit = true;

    protected Vector<TreeReference> references;

    private boolean initialized = false;
    private Action autoLaunchAction;

    private QueryScreen queryScreen;

    public EntityScreen(boolean handleCaseIndex) {
        this.handleCaseIndex = handleCaseIndex;
        entityScreenContext = new EntityScreenContext();
    }

    /**
     * This constructor allows specifying whether to use the complete init or a minimal one
     *
     * @param handleCaseIndex Allow specifying entity by list index rather than unique ID
     * @param needsFullInit   If set to false, the subscreen and referenceMap, used for
     *                        selecting and rendering entity details, will not be created.
     *                        This speeds up initialization but makes further selection impossible.
     */
    public EntityScreen(boolean handleCaseIndex, boolean needsFullInit) {
        this.handleCaseIndex = handleCaseIndex;
        this.needsFullInit = needsFullInit;
        entityScreenContext = new EntityScreenContext();
    }

    public EntityScreen(boolean handleCaseIndex, boolean needsFullInit, SessionWrapper session,
            EntityScreenContext entityScreenContext)
            throws CommCareSessionException {
        this.handleCaseIndex = handleCaseIndex;
        this.needsFullInit = needsFullInit;
        this.setSession(session);
        this.entityScreenContext = entityScreenContext;
    }

    public void evaluateAutoLaunch(String nextInput) throws CommCareSessionException {
        EvaluationContext subContext = getAutoLaunchEvaluationContext(nextInput);
        for (Action action : mShortDetail.getCustomActions(evalContext)) {
            if (action.isAutoLaunchAction(subContext)) {
                // Supply an empty case list so we can "select" from it later using getEntityFromID
                mCurrentScreen = new EntityListSubscreen(mShortDetail, new Vector<>(), evalContext,
                        handleCaseIndex, entityScreenContext);
                this.autoLaunchAction = action;
            }
        }
    }

    @Nonnull
    protected EvaluationContext getAutoLaunchEvaluationContext(String nextInput) {
        EvaluationContext subContext = evalContext.spawnWithCleanLifecycle();
        subContext.setVariable("next_input", nextInput);
        return subContext;
    }

    @Trace
    public void init(SessionWrapper session) throws CommCareSessionException {
        if (initReferences(session)) {
            initListSubScreen();
        }
    }

    /**
     * Initialises EntityListSubscreen for current entity screen
     * Should only be called after initReferences
     * @throws CommCareSessionException
     */
    public void initListSubScreen() throws CommCareSessionException {
        if (!initialized) {
            throw new CommCareSessionException("trying to initialise subscreen before initialising references");
        }

        // if readyToSkip, entity screen will not be displayed. We don't need to init the subscreen
        if (!readyToSkip) {
            // if isDetailScreen or needsFullInit is not set,
            // sub screen is needed to handle actions but we can skip eval refs
            Vector<TreeReference> entityListReferences =
                    !needsFullInit || isDetailScreen() ? new Vector<>() : references;
            mCurrentScreen = new EntityListSubscreen(mShortDetail, entityListReferences, evalContext,
                    handleCaseIndex, entityScreenContext);
        }
    }

    /**
     * Initialises references and referenceMap for current entity screen
     * @param session Current CommCare Session to initialise the screen with
     * @throws CommCareSessionException
     * @return whether we initialised references as part of this call
     */
    public boolean initReferences(SessionWrapper session) throws CommCareSessionException {
        if (initialized) {
            if (session != this.mSession) {
                throw new CommCareSessionException(
                        "Entity screen initialized with two different session wrappers");
            }
            return false;
        }

        this.setSession(session);

        references = expandEntityReferenceSet(evalContext);

        //Pulled from NodeEntityFactory. We should likely replace this whole functonality with
        //that from nodeentityfactory
        QueryContext newContext = evalContext.getCurrentQueryContext()
                .checkForDerivativeContextAndReturn(references.size());

        newContext.setHackyOriginalContextBody(new CurrentModelQuerySet(references));

        evalContext.setQueryContext(newContext);

        if (needsFullInit || references.size() == 1 || shouldAutoSelect()) {
            referenceMap = new Hashtable<>();
            EntityDatum needed = (EntityDatum)session.getNeededDatum();
            for (TreeReference reference : references) {
                referenceMap.put(getReturnValueFromSelection(reference, needed, evalContext), reference);
            }

            // for now override 'here()' with the coords of Sao Paulo, eventually allow dynamic
            // setting
            evalContext.addFunctionHandler(new ScreenUtils.HereDummyFunc(-23.56, -46.66));

            if (shouldAutoSelect()) {
                if (!this.setCurrentScreenToDetail()) {
                    readyToSkip = true;
                }
            }
        }
        initialized = true;
        return true;
    }

    protected boolean shouldAutoSelect() {
        return mNeededDatum.isAutoSelectEnabled() && references.size() == 1;
    }

    /**
     * Auto selects entities for the screen and advances the session
     *
     * @param session Current CommCare Session
     * @return whether the session was advanced by this call
     * @throws CommCareSessionException errors while auto selecting entities
     */
    public boolean autoSelectEntities(SessionWrapper session) throws CommCareSessionException {
        this.setSelectedEntity(references.firstElement());
        if (!setCurrentScreenToDetail()) {
            updateSession(session);
            return true;
        }
        return false;
    }

    protected void setSession(SessionWrapper session) throws CommCareSessionException {
        SessionDatum datum = session.getNeededDatum();
        if (!(datum instanceof EntityDatum)) {
            throw new CommCareSessionException(
                    "Didn't find an entity select action where one is expected.");
        }
        mNeededDatum = (EntityDatum)datum;

        this.mSession = session;
        this.mPlatform = mSession.getPlatform();

        String detailId = mNeededDatum.getShortDetail();
        if (detailId == null) {
            throw new CommCareSessionException(
                    "Can't handle entity selection with blank detail definition for datum "
                            + mNeededDatum.getDataId());
        }

        mShortDetail = this.mPlatform.getDetail(detailId);

        if (mShortDetail == null) {
            throw new CommCareSessionException("Missing detail definition for: " + detailId);
        }

        evalContext = mSession.getEvaluationContext();
    }

    @Trace
    private Vector<TreeReference> expandEntityReferenceSet(EvaluationContext context) {
        return evalContext.expandReference(mNeededDatum.getNodeset());
    }

    @Override
    public boolean shouldBeSkipped() {
        return readyToSkip;
    }

    @Override
    public String getScreenTitle() {
        try {
            return mShortDetail.getTitle().evaluate(evalContext).getName();
        } catch (NoLocalizedTextException nlte) {
            return "Select (error with title string)";
        }
    }

    @Override
    public String getBreadcrumb(String input, UserSandbox sandbox, SessionWrapper session) {
        String caseName = FormDataUtil.getCaseName(sandbox, input);
        return caseName == null ? ScreenUtils.getBestTitle(session) : caseName;
    }

    @Override
    public Subscreen getCurrentScreen() {
        return mCurrentScreen;
    }

    protected String getReturnValueFromSelection(TreeReference contextRef) {
        return getReturnValueFromSelection(contextRef, mNeededDatum, evalContext);
    }

    @Trace
    public static String getReturnValueFromSelection(TreeReference contextRef, EntityDatum needed,
            EvaluationContext context) {
        return DatumUtil.getReturnValueFromSelection(contextRef, needed, context);
    }


    @Trace
    @Override
    protected void updateSession(CommCareSession session) {
        if (executePendingAction(session)) {
            return;
        }

        String selectedValue = getReturnValueFromSelection(mCurrentSelection);
        session.setEntityDatum(mNeededDatum, selectedValue);
    }

    public boolean executePendingAction(CommCareSession session) {
        if (mPendingAction != null) {
            session.executeStackOperations(mPendingAction.getStackOperations(), evalContext);
            return true;
        }
        return false;
    }

    /**
     * Updates entity selected on the screen
     *
     * @param input          id of the entity selected on the screen
     * @param selectedValues should always be null for single-select entity screen
     * @throws CommCareSessionException
     */
    public void updateSelection(String input, @Nullable String[] selectedValues) throws CommCareSessionException {
        setSelectedEntity(input);
        showDetailScreen();
    }

    /**
     * Updates entity selected on the screen
     *
     * @param input        input to the entity selected on the screen
     * @param selectedRefs references for the selected entity, only contains a single element for the
     *                     single-select entity screen
     * @throws CommCareSessionException
     */
    public void updateSelection(String input, TreeReference[] selectedRefs) throws CommCareSessionException {
        if (selectedRefs.length != 1) {
            throw new IllegalArgumentException(
                    "selectedRefs should only contain one element for the single select Entity Screen");
        }
        setSelectedEntity(selectedRefs[0]);
        setCurrentScreenToDetail();
    }

    private void showDetailScreen() throws CommCareSessionException {
        if (isDetailScreen()) {
            // Set entity screen to show detail and redraw
            setCurrentScreenToDetail();
        }
    }

    private boolean isDetailScreen() {
        return entityScreenContext.getDetailSelection() != null;
    }

    @Trace
    public void setSelectedEntity(TreeReference selection) {
        this.mCurrentSelection = selection;
    }

    @Trace
    private void setSelectedEntity(String id) throws CommCareSessionException {
        mCurrentSelection = getEntityReference(id);
        if (this.mCurrentSelection == null) {
            throw new CommCareSessionException("Could not select case " + id + " on this screen. " +
                    " If this error persists please report a bug to CommCareHQ.");
        }
    }

    protected TreeReference getEntityReference(String id) {
        if (referenceMap == null) {
            return mNeededDatum.getEntityFromID(evalContext, id);
        } else {
            return referenceMap.get(id);
        }
    }

    private boolean setCurrentScreenToDetail() throws CommCareSessionException {
        if (mCurrentSelection == null) {
            return false;
        }

        Detail[] longDetailList = getLongDetailList(mCurrentSelection);
        if (longDetailList == null) {
            return false;
        }
        setCurrentScreenToDetail(0);
        return true;
    }

    public void setCurrentScreenToDetail(int index) throws CommCareSessionException {
        EvaluationContext subContext = new EvaluationContext(evalContext, this.mCurrentSelection);
        Detail[] longDetailList = getLongDetailList(this.mCurrentSelection);
        TreeReference detailNodeset = longDetailList[index].getNodeset();
        if (detailNodeset != null) {
            TreeReference contextualizedNodeset = detailNodeset.contextualize(this.mCurrentSelection);
            this.mCurrentScreen = new EntityListSubscreen(longDetailList[index],
                    subContext.expandReference(contextualizedNodeset), subContext, handleCaseIndex,
                    entityScreenContext);
        } else {
            this.mCurrentScreen = new EntityDetailSubscreen(index, longDetailList[index],
                    subContext, getDetailListTitles(subContext, this.mCurrentSelection));
        }
    }

    public Detail[] getLongDetailList(TreeReference ref) {
        Detail d = getLongDetail();
        if (d == null) return null;

        EvaluationContext contextForChildDetailDisplayConditions =
                EntityUtil.prepareCompoundEvaluationContext(ref, d, evalContext);

        Detail[] longDetailList = d.getDisplayableChildDetails(contextForChildDetailDisplayConditions);
        if (longDetailList == null || longDetailList.length == 0) {
            longDetailList = new Detail[]{d};
        }
        return longDetailList;
    }

    public Detail getLongDetail() {
        String longDetailId = this.mNeededDatum.getLongDetail();
        if (longDetailId == null) {
            return null;
        }
        return mPlatform.getDetail(longDetailId);
    }

    @Nullable
    public Endpoint getEndpoint(String endpointId) {
        return mPlatform.getEndpoint(endpointId);
    }

    public String[] getDetailListTitles(EvaluationContext subContext, TreeReference reference) {
        Detail[] mLongDetailList = getLongDetailList(reference);
        String[] titles = new String[mLongDetailList.length];
        for (int i = 0; i < mLongDetailList.length; ++i) {
            titles[i] = mLongDetailList[i].getTitle().getText().evaluate(subContext);
        }
        return titles;
    }

    public void setPendingAction(Action pendingAction) {
        this.mPendingAction = pendingAction;
    }

    public Detail getShortDetail() {
        return mShortDetail;
    }

    public SessionWrapper getSession() {
        return mSession;
    }

    public void printNodesetExpansionTrace(EvaluationTraceReporter reporter) {
        evalContext.setDebugModeOn(reporter);
        this.expandEntityReferenceSet(evalContext);
        InstrumentationUtils.printAndClearTraces(reporter, "Entity Nodeset");
    }

    public TreeReference resolveTreeReference(String reference) {
        return referenceMap.get(reference);
    }

    public EvaluationContext getEvalContext() {
        return evalContext;
    }

    public TreeReference getCurrentSelection() {
        return mCurrentSelection;
    }

    public Vector<TreeReference> getReferences() {
        return references;
    }

    public Action getAutoLaunchAction() {
        return autoLaunchAction;
    }

    @Override
    public String toString() {
        return "EntityScreen [id=" + mNeededDatum.getDataId() + ", selection=" + mCurrentSelection + "]";
    }

    // Used by Formplayer
    @SuppressWarnings("unused")
    public Hashtable<String, TreeReference> getReferenceMap() {
        return referenceMap;
    }

    public boolean referencesContainStep(String stepValue) {
        if (referenceMap != null) {
            return referenceMap.containsKey(stepValue);
        }
        for (TreeReference ref : references) {
            String id = getReturnValueFromSelection(ref);
            if (id.equals(stepValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the datum required by the given CommCare Session. It's generally used during session replays when
     * we want to update the datum directly with the pre-validated input wihout doing any other input handling
     *
     * @param session Current Commcare Session that we need to update with given input
     * @param input   Value of the datum required by the given CommCare Session
     */
    public void updateDatum(CommCareSession session, String input) {
        session.setEntityDatum(session.getNeededDatum(), input);
    }

    /**
     * Handle auto-launch actions for EntityScreens
     *
     * @return true if the session was advanced
     * @throws CommCareSessionException if there was an error during evaluation of auto launch action
     */
    public boolean evalAndExecuteAutoLaunchAction(String nextInput, CommCareSession session)
            throws CommCareSessionException {
        evaluateAutoLaunch(nextInput);
        if (getAutoLaunchAction() != null) {
            setPendingAction(getAutoLaunchAction());
            executePendingAction(session);
            return true;
        }
        return false;
    }

    public QueryScreen getQueryScreen() {
        return queryScreen;
    }

    public void setQueryScreen(QueryScreen queryScreen) {
        this.queryScreen = queryScreen;
    }

    public EntityScreenContext getEntityScreenContext() {
        return entityScreenContext;
    }
}
