package org.commcare.session;

import org.commcare.modern.util.Pair;
import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.SessionDatum;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Manager for remote query datums; get/answer user prompts and build
 * resulting query url.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class RemoteQuerySessionManager {
    private final RemoteQueryDatum queryDatum;
    private final EvaluationContext evaluationContext;
    private final Hashtable<String, String> userAnswers =
            new Hashtable<>();

    private RemoteQuerySessionManager(RemoteQueryDatum queryDatum,
                                      EvaluationContext evaluationContext) {
        this.queryDatum = queryDatum;
        this.evaluationContext = evaluationContext;
    }

    public static RemoteQuerySessionManager buildQuerySessionManager(CommCareSession session,
                                                                     EvaluationContext sessionContext) {
        SessionDatum datum;
        try {
            datum = session.getNeededDatum();
        } catch (IllegalStateException e) {
            // tried loading session info when it wasn't there
            return null;
        }
        if (datum instanceof RemoteQueryDatum) {
            return new RemoteQuerySessionManager((RemoteQueryDatum)datum, sessionContext);
        } else {
            return null;
        }
    }

    public OrderedHashtable<String, DisplayUnit> getNeededUserInputDisplays() {
        return queryDatum.getUserQueryPrompts();
    }

    public Hashtable<String, String> getUserAnswers() {
        return userAnswers;
    }

    public void clearAnswers() {
        userAnswers.clear();
    }

    public void answerUserPrompt(String key, String answer) {
        userAnswers.put(key, answer);
    }

    public URL getBaseUrl() {
        return queryDatum.getUrl();
    }

    public Hashtable<String, String> getRawQueryParams() {
        Hashtable<String, String> params = new Hashtable<>();
        Hashtable<String, XPathExpression> hiddenQueryValues = queryDatum.getHiddenQueryValues();
        for (Enumeration e = hiddenQueryValues.keys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String evaluatedExpr = evalXpathExpression(hiddenQueryValues.get(key), evaluationContext);
            params.put(key, evaluatedExpr);
        }
        for (Enumeration e = userAnswers.keys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            params.put(key, userAnswers.get(key));
        }
        return params;
    }

    public static String evalXpathExpression(XPathExpression expr,
                                             EvaluationContext evaluationContext) {
        return FunctionUtils.toString(expr.eval(evaluationContext));
    }

    /**
     * @return Data instance built from xml stream or the error message raised during parsing
     */
    public Pair<ExternalDataInstance, String> buildExternalDataInstance(InputStream instanceStream) {
        TreeElement root;
        try {
            KXmlParser baseParser = ElementParser.instantiateParser(instanceStream);
            root = new TreeElementParser(baseParser, 0, queryDatum.getDataId()).parse();
        } catch (InvalidStructureException | IOException
                | XmlPullParserException | UnfullfilledRequirementsException e) {
            e.printStackTrace();
            return new Pair<>(null, e.getMessage());
        }
        return new Pair<>(ExternalDataInstance.buildFromRemote(queryDatum.getDataId(), root, queryDatum.useCaseTemplate()), "");
    }

}
