package org.commcare.util.cli;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.util.mocks.SessionWrapper;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
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
    int currentDetail;

    private String mTitle;

    private SessionWrapper mSession;
    private MockUserDataSandbox mSandbox;
    private CommCarePlatform mPlatform;

    private Detail mShortDetail;
    private Detail[] mLongDetailList;

    private SessionDatum mNeededDatum;

    Subscreen<EntityScreen> mCurrentScreen;

    public void init(CommCarePlatform platform, SessionWrapper session, MockUserDataSandbox sandbox) throws CommCareSessionException {
        mNeededDatum = session.getNeededDatum();

        this.mSandbox = sandbox;
        this.mSession = session;
        this.mPlatform = platform;

        String detailId = mNeededDatum.getShortDetail();
        if(detailId == null) {
            throw new CommCareSessionException("Can't handle entity selection with blank detail definition for datum " + mNeededDatum.getDataId());
        }

        mShortDetail = platform.getDetail(detailId);

        if(mShortDetail == null) {
            throw new CommCareSessionException("Missing detail definition for: " + detailId);
        }

        mCurrentScreen = new EntityListSubscreen(mShortDetail, mNeededDatum, session.getEvaluationContext());
    }

    @Override
    protected String getScreenTitle() {
        return mShortDetail.getTitle().evaluate(mSession.getEvaluationContext()).getName();
    }

    @Override
    public Subscreen getCurrentScreen() {
        return mCurrentScreen;
    }

    private String getReturnValueFromSelection(TreeReference contextRef, SessionDatum needed, EvaluationContext context) {
        // grab the session's (form) element reference, and load it.
        TreeReference elementRef =
                XPathReference.getPathExpr(needed.getValue()).getReference(true);

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
        String selectedValue = this.getReturnValueFromSelection(this.mCurrentSelection,
                mNeededDatum, mSession.getEvaluationContext());
        session.setDatum(mNeededDatum.getDataId(), selectedValue);

    }

    public void setHighlightedEntity(TreeReference selection) {
        this.mCurrentSelection = selection;
    }

    public void initDetailScreens() {
        Detail d = mPlatform.getDetail(this.mNeededDatum.getLongDetail());
        if(d == null) {
            mLongDetailList = null;
            return;
        }
        mLongDetailList = d.getDetails();
        if(mLongDetailList == null || mLongDetailList.length == 0) {
            mLongDetailList = new Detail[] {d};
        }
    }

    public boolean setCurrentScreenToDetail() {
        initDetailScreens();

        if(mLongDetailList == null) {
            return false;
        }

        setCurrentScreenToDetail(0);
        return true;
    }

    public void setCurrentScreenToDetail(int index) {
        EvaluationContext subContext = new EvaluationContext(mSession.getEvaluationContext(), this.mCurrentSelection);

        this.mCurrentScreen = new EntityDetailSubscreen(index, this.mLongDetailList[index], subContext, getDetailListTitles(subContext));
    }

    private String[] getDetailListTitles(EvaluationContext subContext) {
        String[] titles = new String[mLongDetailList.length];
        for(int i = 0 ; i < mLongDetailList.length ; ++i) {
            titles[i] = this.mLongDetailList[i].getTitle().getText().evaluate(subContext);
        }
        return titles;
    }
}
