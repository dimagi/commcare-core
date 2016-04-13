package org.commcare.session;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.RemoteQueryDatum;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Manager for remote query datums; get/answer user prompts and build
 * resulting query url.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class RemoteQuerySession {
    private final RemoteQueryDatum queryDatum;
    private final Hashtable<String, String> userAnswers = new Hashtable<String, String>();
    private final EvaluationContext evaluationContext;

    public RemoteQuerySession(RemoteQueryDatum queryDatum, EvaluationContext evaluationContext) {
        this.queryDatum = queryDatum;
        this.evaluationContext = evaluationContext;
    }

    public Hashtable<String, DisplayUnit> getNeededUserInputDisplays() {
        return queryDatum.getUserQueryPrompts();
    }

    public void answerUserPrompt(String key, String answer) {
        userAnswers.put(key, answer);
    }

    private String calculateHidden(XPathExpression expr) {
        return XPathFuncExpr.toString(expr.eval(evaluationContext));
    }

    public boolean areAllUserPromptsAnswered() {
        for (Enumeration e = queryDatum.getUserQueryPrompts().keys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            if (!userAnswers.containsKey(key)) {
                return false;
            }
        }
        return true;
    }
}
