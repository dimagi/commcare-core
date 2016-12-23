package org.commcare.util;

import org.commcare.cases.model.Case;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.ComputedDatum;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

/**
 * Use the session state descriptor attached to saved forms to load case
 * information, such as the case name.
 */
public class FormDataUtil {
    public static String getTitleFromSession(UserSandbox userSandbox,
                                             CommCareSession session,
                                             EvaluationContext evalContext) {
        CommCareSession sessionCopy = new CommCareSession(session);
        String datumValue = null;

        while (sessionCopy.getFrame().getSteps().size() > 0) {
            SessionDatum datum = sessionCopy.getNeededDatum();
            if (isCaseIdComputedDatum(datum, datumValue, sessionCopy.getPoppedStep())) {
                // Loads case id for forms that create cases, since case id was
                // a computed datum injected into the form. Assumes that it was
                // the 1st computed datum... another good heuristic would be
                // session.getPoppedStep().getId().startsWith("case_id")
                datumValue = sessionCopy.getPoppedStep().getValue();
            } else if (datum instanceof EntityDatum) {
                String tmpDatumValue = sessionCopy.getPoppedStep().getValue();
                if (tmpDatumValue != null) {
                    datumValue = tmpDatumValue;
                }
                if (((EntityDatum)datum).getLongDetail() == null) {
                    // In the absence of a case detail, use the plain case name as the title
                    break;
                } else {
                    return loadTitleFromEntity((EntityDatum)datum, datumValue, evalContext, sessionCopy, userSandbox);
                }
            }
            sessionCopy.popStep(evalContext);
        }

        if (datumValue == null) {
            return null;
        } else {
            return getCaseName(userSandbox, datumValue);
        }
    }

    private static boolean isCaseIdComputedDatum(SessionDatum datum,
                                                 String currentDatumValue,
                                                 StackFrameStep poppedStep) {
        return datum instanceof ComputedDatum &&
                (currentDatumValue == null || poppedStep.getId().equals(datum.getDataId()));
    }

    private static String loadTitleFromEntity(EntityDatum entityDatum, String value,
                                              EvaluationContext evalContext,
                                              CommCareSession sessionCopy,
                                              UserSandbox userSandbox) {
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
