package org.commcare.util;

import org.commcare.suite.model.SessionDatum;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathPathExpr;

public class DatumUtil {

    public static String getReturnValueFromSelection(TreeReference contextRef, SessionDatum needed,
            EvaluationContext context) {
        return getReturnValueFromSelection(contextRef, needed.getValue(), context);
    }

    public static String getReturnValueFromSelection(TreeReference contextRef, String value,
            EvaluationContext context) {
        return getReturnValueFromSelection(contextRef, XPathReference.getPathExpr(value), context);
    }

    public static String getReturnValueFromSelection(TreeReference contextRef, XPathPathExpr valueExpr,
            EvaluationContext context) {

        TreeReference elementRef = valueExpr.getReference();

        AbstractTreeElement element = context.resolveReference(elementRef.contextualize(contextRef));

        if (element != null && element.getValue() != null) {
            return element.getValue().uncast().getString();
        }
        return "";
    }
}
