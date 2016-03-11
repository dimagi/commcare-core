package org.commcare.util.cli;

import org.commcare.api.session.SessionWrapper;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.CommCarePlatform;
import org.commcare.session.CommCareSession;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.model.xform.XPathReference;

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
    private Detail[] mLongDetailList;

    private SessionDatum mNeededDatum;
    private Action mPendingAction;

    private Subscreen<EntityScreen> mCurrentScreen;

    public void init(SessionWrapper session) throws CommCareSessionException {
        mNeededDatum = session.getNeededDatum();

        System.out.println("Needed Datum: " + mNeededDatum);

        this.mSession = session;
        this.mPlatform = mSession.getPlatform();

        String detailId = mNeededDatum.getShortDetail();
        if(detailId == null) {
            throw new CommCareSessionException("Can't handle entity selection with blank detail definition for datum " + mNeededDatum.getDataId());
        }

        mShortDetail = this.mPlatform.getDetail(detailId);

        if(mShortDetail == null) {
            throw new CommCareSessionException("Missing detail definition for: " + detailId);
        }

        EvaluationContext ec = session.getEvaluationContext();
        mCurrentScreen = new EntityListSubscreen(mShortDetail, ec.expandReference(mNeededDatum.getNodeset()), ec);
    }

    @Override
    protected String getScreenTitle() {
        try {
            return mShortDetail.getTitle().evaluate(mSession.getEvaluationContext()).getName();
        }catch (NoLocalizedTextException nlte) {
            return "Select (error with title string)";
        }
    }

    @Override
    public Subscreen getCurrentScreen() {
        return mCurrentScreen;
    }

    private String getReturnValueFromSelection(TreeReference contextRef, SessionDatum needed, EvaluationContext context) {
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
        if(mPendingAction != null) {
            session.executeStackOperations(mPendingAction.getStackOperations(), mSession.getEvaluationContext());
            return;
        }

        String selectedValue = this.getReturnValueFromSelection(this.mCurrentSelection,
                mNeededDatum, mSession.getEvaluationContext());
        session.setDatum(mNeededDatum.getDataId(), selectedValue);

    }

    public void setHighlightedEntity(TreeReference selection) {
        this.mCurrentSelection = selection;
    }

    private void initDetailScreens() {
        String longDetailId = this.mNeededDatum.getLongDetail();
        if(longDetailId == null) {
            mLongDetailList = null;
            return;
        }
        Detail d = mPlatform.getDetail(longDetailId);
        if(d == null) {
            mLongDetailList = null;
            return;
        }
        mLongDetailList = d.getDetails();
        if(mLongDetailList == null || mLongDetailList.length == 0) {
            mLongDetailList = new Detail[] {d};
        }
    }

    public boolean setCurrentScreenToDetail() throws CommCareSessionException {
        initDetailScreens();

        if(mLongDetailList == null) {
            return false;
        }

        setCurrentScreenToDetail(0);
        return true;
    }

    public void setCurrentScreenToDetail(int index) throws CommCareSessionException {
        EvaluationContext subContext = new EvaluationContext(mSession.getEvaluationContext(), this.mCurrentSelection);

        TreeReference detailNodeset = this.mLongDetailList[index].getNodeset();
        if (detailNodeset != null) {
            TreeReference contextualizedNodeset = detailNodeset.contextualize(this.mCurrentSelection);
            this.mCurrentScreen = new EntityListSubscreen(this.mLongDetailList[index], subContext.expandReference(contextualizedNodeset), subContext);
        }
        else {
            this.mCurrentScreen = new EntityDetailSubscreen(index, this.mLongDetailList[index], subContext, getDetailListTitles(subContext));
        }
    }

    private String[] getDetailListTitles(EvaluationContext subContext) {
        String[] titles = new String[mLongDetailList.length];
        for(int i = 0 ; i < mLongDetailList.length ; ++i) {
            titles[i] = this.mLongDetailList[i].getTitle().getText().evaluate(subContext);
        }
        return titles;
    }

    public void setPendingAction(Action pendingAction) {
        this.mPendingAction = pendingAction;
    }
}
