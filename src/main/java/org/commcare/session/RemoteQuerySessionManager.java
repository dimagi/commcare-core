package org.commcare.session;

import org.commcare.cases.util.StringUtils;
import org.commcare.modern.util.Pair;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.SessionDatum;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.utils.ItemSetUtils;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_SELECT1;

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
                                      EvaluationContext evaluationContext) throws XPathException {
        this.queryDatum = queryDatum;
        this.evaluationContext = evaluationContext;
        initUserAnswers();
    }

    private void initUserAnswers() throws XPathException {
        OrderedHashtable<String, QueryPrompt> queryPrompts = queryDatum.getUserQueryPrompts();
        for (Enumeration en = queryPrompts.keys(); en.hasMoreElements(); ) {
            String promptId = (String)en.nextElement();
            QueryPrompt prompt = queryPrompts.get(promptId);
            String defaultValue = "";
            if(prompt.getDefaultValueExpr() != null) {
                defaultValue = FunctionUtils.toString(prompt.getDefaultValueExpr().eval(evaluationContext));
            }
            userAnswers.put(prompt.getKey(), defaultValue);
        }
    }

    public static RemoteQuerySessionManager buildQuerySessionManager(CommCareSession session,
                                                                     EvaluationContext sessionContext)  throws XPathException {
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

    public OrderedHashtable<String, QueryPrompt> getNeededUserInputDisplays() {
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
            String value = userAnswers.get(key);
            if (!StringUtils.isEmpty(value)) {
                params.put(key, userAnswers.get(key));
            }
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

    public void populateItemSetChoices(QueryPrompt queryPrompt) {
        EvaluationContext evalContextWithAnswers = evaluationContext.spawnWithCleanLifecycle();
        evalContextWithAnswers.setVariables(userAnswers);
        ItemSetUtils.populateDynamicChoices(queryPrompt.getItemsetBinding(), evalContextWithAnswers);
    }

    // loops over query prompts and validates selection until all selections are valid
    public void refreshItemSetChoices(Hashtable<String, String> userAnswers) {
        OrderedHashtable<String, QueryPrompt> userInputDisplays = getNeededUserInputDisplays();
        boolean dirty = true;
        int index = 0;
        while (dirty) {
            if (index == userInputDisplays.size()) {
                // loop has already run as many times as no of questions and we are still dirty
                throw new RuntimeException("Invalid itemset state encountered while trying to refresh itemset choices");
            }
            dirty = false;
            for (Enumeration en = userInputDisplays.keys(); en.hasMoreElements(); ) {
                String promptId = (String)en.nextElement();
                QueryPrompt queryPrompt = userInputDisplays.get(promptId);
                if (queryPrompt.getInput() != null && queryPrompt.getInput().contentEquals(INPUT_TYPE_SELECT1)) {
                    String answer = userAnswers.get(promptId);
                    populateItemSetChoices(queryPrompt);
                    Vector<SelectChoice> items = queryPrompt.getItemsetBinding().getChoices();
                    if (!checkForValidSelectValue(items, answer)) {
                        // if it's not a valid select value, blank it out
                        userAnswers.put(promptId, "");
                        dirty = true;
                    }
                }
            }
            index++;
        }
    }

    // checks if @param{value} is one of the select choices give in @param{items}
    private boolean checkForValidSelectValue(Vector<SelectChoice> items, String value) {
        // blank is always a valid choice
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getValue().contentEquals(value)) {
                return true;
            }
        }
        return false;
    }
}
