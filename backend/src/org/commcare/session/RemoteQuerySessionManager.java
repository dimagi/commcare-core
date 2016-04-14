package org.commcare.session;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.RemoteQueryDatum;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Manager for remote query datums; get/answer user prompts and build
 * resulting query url.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class RemoteQuerySessionManager {
    private final RemoteQueryDatum queryDatum;
    private final Hashtable<String, String> userAnswers = new Hashtable<String, String>();
    private final EvaluationContext evaluationContext;

    public RemoteQuerySessionManager(RemoteQueryDatum queryDatum, EvaluationContext evaluationContext) {
        this.queryDatum = queryDatum;
        this.evaluationContext = evaluationContext;
    }

    public Hashtable<String, DisplayUnit> getNeededUserInputDisplays() {
        return queryDatum.getUserQueryPrompts();
    }

    public Hashtable<String, String> getUserAnswers() {
        return userAnswers;
    }

    public void answerUserPrompt(String key, String answer) {
        userAnswers.put(key, answer);
    }

    public String getBaseUrl() {
        return queryDatum.getValue();
    }

    public Hashtable<String, String> getRawQueryParams() {
        Hashtable<String, String> params = new Hashtable<String, String>();
        Hashtable<String, XPathExpression> hiddenQueryValues = queryDatum.getHiddenQueryValues();
        for (Enumeration e = hiddenQueryValues.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            String evaluatedExpr = calculateHidden(hiddenQueryValues.get(key), evaluationContext);
            params.put(key, evaluatedExpr);
        }
        for (Enumeration e = userAnswers.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            params.put(key, userAnswers.get(key));
        }
        return params;
    }

    public static String calculateHidden(XPathExpression expr,
                                         EvaluationContext evaluationContext) {
        return XPathFuncExpr.toString(expr.eval(evaluationContext));
    }

    public boolean areAllUserPromptsAnswered() {
        for (Enumeration e = queryDatum.getUserQueryPrompts().keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            if (!userAnswers.containsKey(key)) {
                return false;
            }
        }
        return true;
    }
}
