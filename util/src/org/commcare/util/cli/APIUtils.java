package org.commcare.util.cli;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.trace.StringEvaluationTraceSerializer;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.text.Normalizer;

/**
 * Created by willpride on 10/9/15.
 */
public class APIUtils {

    public static String evalExpression(String xpath, EvaluationContext ec, FormEntryController fec) {
        System.out.println(xpath);
        XPathExpression expr;
        try {
            expr = XPathParseTool.parseXPath(xpath);
        } catch (XPathSyntaxException e) {
            System.out.println("Error (parse): " + e.getMessage());
            return null;
        }

        //See if we're on a valid index, if so use that as our EC base
        if(fec != null) {
            FormIndex current = fec.getModel().getFormIndex();
            if (current.isInForm()) {
                ec = new EvaluationContext(ec, current.getReference());
            }
        }
        try {
            Object val = expr.eval(ec);
            System.out.println(getDisplayString(val));
            return getDisplayString(val);
        } catch (Exception e) {
            System.out.println("Error  (eval): " + e.getMessage());
            return null;
        }
    }

    public static String evalExpression(String xpath, EvaluationContext ec) {
        return evalExpression(xpath, ec, null);
    }

    public static String evalExpression(String xpath, FormEntryController fec) {
        return evalExpression(xpath, fec.getModel().getForm().getEvaluationContext(), fec);
    }

    private static String getDisplayString(Object value) {
        if (value instanceof XPathNodeset) {
            return XPathFuncExpr.getSerializedNodeset((XPathNodeset) value);
        } else {
            return XPathFuncExpr.toString(value);
        }
    }


}
