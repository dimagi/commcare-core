package org.commcare.util;

import org.commcare.cases.model.Case;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FormDataUtil {
    public static String getTitleFromSession(UserSandbox userSandbox,
                                             CommCareSession session,
                                             EvaluationContext evalContext) {
        CommCareSession sessionCopy = new CommCareSession(session);

        EntityDatum entityDatum =
                findDatumWithLongDetail(sessionCopy, evalContext);
        if (entityDatum == null || sessionCopy.getFrame().getSteps().size() == 0) {
            return null;
        }

        //Get the value that was chosen for this item
        String value = sessionCopy.getPoppedStep().getValue();

        // Now determine what nodeset that was going to be used to load this select
        TreeReference elem = entityDatum.getEntityFromID(evalContext, value);
        if (elem == null) {
            return null;
        }

        Text detailText = sessionCopy.getDetail(entityDatum.getLongDetail()).getTitle().getText();
        boolean isPrettyPrint = true;

        //CTS: this is... not awesome.
        //But we're going to use this to test whether we _need_ an evaluation context
        //for this. (If not, the title doesn't have prettyprint for us)
        try {
            String outcome = detailText.evaluate();
            if (outcome != null) {
                isPrettyPrint = false;
            }
        } catch (Exception e) {
            //Cool. Got us a fancy string.
        }

        if (isPrettyPrint) {
            // Get the detail title for that element
            EvaluationContext elementContext = new EvaluationContext(evalContext, elem);
            return detailText.evaluate(elementContext);
        } else {
            return getCaseName(userSandbox, value);
        }
    }

    private static EntityDatum findDatumWithLongDetail(CommCareSession session,
                                                       EvaluationContext evaluationContext) {
        while (session.getFrame().getSteps().size() > 0) {
            SessionDatum datum = session.getNeededDatum();
            if (datum instanceof EntityDatum && ((EntityDatum)datum).getLongDetail() != null) {
                return (EntityDatum)datum;
            }
            session.stepBack(evaluationContext);
        }
        return null;
    }

    private static String getCaseName(UserSandbox userSandbox, String caseId) {
        try {
            Case ourCase = userSandbox.getCaseStorage().getRecordForValue(Case.INDEX_CASE_ID, caseId);
            if (ourCase != null) {
                return ourCase.getName();
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
