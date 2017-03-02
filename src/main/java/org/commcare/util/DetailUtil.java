package org.commcare.util;

import org.commcare.suite.model.Detail;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

/**
 * Created by amstone326 on 3/2/17.
 */

public class DetailUtil {

    /**
     * Note that if isChild is false, then parentDetail is just the detail for
     * @param childReference
     * @param isChild - the detail for which we are retrieving a context has a parent detail
     * @param parentDetail - the parent detail, or null if isChild
     * @return
     */
    public static EvaluationContext getEntityFactoryContext(TreeReference childReference,
                                                            boolean isChild,
                                                            Detail parentDetail,
                                                            EvaluationContext baseContext) {
        if (isChild) {
            return prepareCompoundEvaluationContext(childReference, parentDetail, baseContext);
        } else {
            return baseContext;
        }
    }

    /**
     * Creates an evaluation context which is preloaded with all of the variables and context from
     * the parent detail definition.
     *
     * @param childReference The qualified reference for the nodeset in the parent detail
     * @return An evaluation context ready to be used as the base of the subnode detail, including
     * any variable definitions included by the parent.
     */
    private static EvaluationContext prepareCompoundEvaluationContext(TreeReference childReference,
                                                                      Detail parentDetail,
                                                                      EvaluationContext baseContext) {
        EvaluationContext parentDetailContext = new EvaluationContext(baseContext, childReference);
        parentDetail.populateEvaluationContextVariables(parentDetailContext);
        return parentDetailContext;
    }
}
