package org.commcare.util.screen;

import org.commcare.cases.entity.EntityUtil;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.EvaluationTraceReporter;
import org.javarosa.core.model.utils.InstrumentationUtils;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.model.xform.XPathReference;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Compound Screen to select an entity from a list and then display the one or more details that
 * are associated with the entity.
 *
 * Does not currently support tile based selects
 *
 * @author ctsims
 */
public class EntityScreen extends CompoundScreenHost {

    private TreeReference mCurrentSelection;

    private SessionWrapper mSession;
    private CommCarePlatform mPlatform;

    private Detail mShortDetail;

    private EntityDatum mNeededDatum;
    private Action mPendingAction;

    private Subscreen<EntityScreen> mCurrentScreen;

    private boolean readyToSkip = false;
    private EvaluationContext evalContext;

    private Hashtable<String, TreeReference> referenceMap;

    public void init(SessionWrapper session) throws CommCareSessionException {
        SessionDatum datum = session.getNeededDatum();
        if (!(datum instanceof EntityDatum)) {
            throw new CommCareSessionException("Didn't find an entity select action where one is expected.");
        }
        mNeededDatum = (EntityDatum)datum;

        this.mSession = session;
        this.mPlatform = mSession.getPlatform();

        String detailId = mNeededDatum.getShortDetail();
        if (detailId == null) {
            throw new CommCareSessionException("Can't handle entity selection with blank detail definition for datum " + mNeededDatum.getDataId());
        }

        mShortDetail = this.mPlatform.getDetail(detailId);

        if (mShortDetail == null) {
            throw new CommCareSessionException("Missing detail definition for: " + detailId);
        }

        evalContext = mSession.getEvaluationContext();

        Vector<TreeReference> references = expandEntityReferenceSet(evalContext);
        referenceMap = new Hashtable<>();
        for(TreeReference reference: references) {
            referenceMap.put(getReturnValueFromSelection(reference, (EntityDatum) session.getNeededDatum(), evalContext), reference);
        }

        // for now override 'here()' with the coords of Sao Paulo, eventually allow dynamic setting
        evalContext.addFunctionHandler(new ScreenUtils.HereDummyFunc(-23.56, -46.66));

        if (mNeededDatum.isAutoSelectEnabled() && references.size() == 1) {
            this.setHighlightedEntity(references.firstElement());
            if (!this.setCurrentScreenToDetail()) {
                this.updateSession(session);
                readyToSkip = true;
            }
        } else {
            mCurrentScreen = new EntityListSubscreen(mShortDetail, references, evalContext);
        }
    }

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
    public Subscreen getCurrentScreen() {
        return mCurrentScreen;
    }

    public static String getReturnValueFromSelection(TreeReference contextRef, EntityDatum needed, EvaluationContext context) {
        // grab the session's (form) element reference, and load it.
        TreeReference elementRef =
                XPathReference.getPathExpr(needed.getValue()).getReference();

        AbstractTreeElement element =
                context.resolveReference(elementRef.contextualize(contextRef));

        String value = "";
        // get the case id and add it to the intent
        if (element != null && element.getValue() != null) {
            value = element.getValue().uncast().getString();
        }
        return value;
    }

    @Override
    protected void updateSession(CommCareSession session) {
        if (mPendingAction != null) {
            session.executeStackOperations(mPendingAction.getStackOperations(), evalContext);
            return;
        }

        String selectedValue = this.getReturnValueFromSelection(this.mCurrentSelection,
                mNeededDatum, evalContext);
        session.setDatum(mNeededDatum.getDataId(), selectedValue);

    }

    public void setHighlightedEntity(TreeReference selection) {
        this.mCurrentSelection = selection;
    }

    public void setHighlightedEntity(String id) throws CommCareSessionException {
        this.mCurrentSelection = referenceMap.get(id);
        if (this.mCurrentSelection == null) {
            throw new CommCareSessionException("EntityScreen " + this.toString() + " could not select case " + id + "." +
                    " If this error persists please report a bug to CommCareHQ.");
        }
    }

    public boolean setCurrentScreenToDetail() throws CommCareSessionException {
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
            this.mCurrentScreen = new EntityListSubscreen(longDetailList[index], subContext.expandReference(contextualizedNodeset), subContext);
        } else {
            this.mCurrentScreen = new EntityDetailSubscreen(index, longDetailList[index], subContext, getDetailListTitles(subContext, this.mCurrentSelection));
        }
    }

    public Detail[] getLongDetailList(TreeReference ref){
        Detail[] longDetailList;
        String longDetailId = this.mNeededDatum.getLongDetail();
        if (longDetailId == null) {
            return null;
        }
        Detail d = mPlatform.getDetail(longDetailId);
        if (d == null) {
            return null;
        }

        EvaluationContext contextForChildDetailDisplayConditions =
                EntityUtil.prepareCompoundEvaluationContext(ref, d, evalContext);

        longDetailList = d.getDisplayableChildDetails(contextForChildDetailDisplayConditions);
        if (longDetailList == null || longDetailList.length == 0) {
            longDetailList = new Detail[]{d};
        }
        return longDetailList;
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

    public Detail getShortDetail(){
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

    @Override
    public String toString() {
        return "EntityScreen [Detail=" + mShortDetail + ", selection=" + mCurrentSelection + "]";
    }

    public Hashtable<String, TreeReference> getReferenceMap() {
        return referenceMap;
    }
}
