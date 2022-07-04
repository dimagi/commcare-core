package org.commcare.session;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import org.commcare.cases.util.StringUtils;
import org.commcare.data.xml.VirtualInstances;
import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.SessionDatum;
import org.javarosa.core.model.ItemsetBinding;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.utils.ItemSetUtils;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Manager for remote query datums; get/answer user prompts and build
 * resulting query url.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class RemoteQuerySessionManager {
    // used to parse multi-select choices
    public static final String ANSWER_DELIMITER = "#,#";

    private final RemoteQueryDatum queryDatum;
    private final EvaluationContext evaluationContext;
    private final Hashtable<String, String> userAnswers =
            new Hashtable<>();
    private final List<String> supportedPrompts;

    private RemoteQuerySessionManager(RemoteQueryDatum queryDatum,
            EvaluationContext evaluationContext,
            List<String> supportedPrompts) throws XPathException {
        this.queryDatum = queryDatum;
        this.evaluationContext = evaluationContext;
        this.supportedPrompts = supportedPrompts;
        initUserAnswers();
    }

    private void initUserAnswers() throws XPathException {
        OrderedHashtable<String, QueryPrompt> queryPrompts = queryDatum.getUserQueryPrompts();
        for (Enumeration en = queryPrompts.keys(); en.hasMoreElements(); ) {
            String promptId = (String)en.nextElement();
            QueryPrompt prompt = queryPrompts.get(promptId);

            if (isPromptSupported(prompt) && prompt.getDefaultValueExpr() != null) {
                userAnswers.put(prompt.getKey(),
                        FunctionUtils.toString(prompt.getDefaultValueExpr().eval(evaluationContext)));
            }

        }
    }

    public static RemoteQuerySessionManager buildQuerySessionManager(CommCareSession session,
            EvaluationContext sessionContext,
            List<String> supportedPrompts) throws XPathException {
        SessionDatum datum;
        try {
            datum = session.getNeededDatum();
        } catch (IllegalStateException e) {
            // tried loading session info when it wasn't there
            return null;
        }
        if (datum instanceof RemoteQueryDatum) {
            return new RemoteQuerySessionManager((RemoteQueryDatum)datum, sessionContext, supportedPrompts);
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

    /**
     * Register a non-null value as an answer for the given key.
     * If value is null, removes the corresponding answer
     */
    public void answerUserPrompt(String key, @Nullable String value) {
        if (value == null) {
            userAnswers.remove(key);
        } else {
            userAnswers.put(key, value);
        }
    }

    public URL getBaseUrl() {
        return queryDatum.getUrl();
    }

    /**
     * @param skipDefaultPromptValues don't apply the default value expressions for query prompts
     * @return filters to be applied to case search uri as query params
     */
    public Multimap<String, String> getRawQueryParams(boolean skipDefaultPromptValues) {
        EvaluationContext evalContextWithAnswers = getEvaluationContextWithUserInputInstance();

        Multimap<String, String> params = ArrayListMultimap.create();
        List<QueryData> hiddenQueryValues = queryDatum.getHiddenQueryValues();
        for (QueryData queryData : hiddenQueryValues) {
            params.putAll(queryData.getKey(), queryData.getValues(evalContextWithAnswers));
        }

        if (!skipDefaultPromptValues) {
            for (Enumeration e = userAnswers.keys(); e.hasMoreElements(); ) {
                String key = (String)e.nextElement();
                String value = userAnswers.get(key);
                QueryPrompt prompt = queryDatum.getUserQueryPrompts().get(key);
                XPathExpression excludeExpr = prompt.getExclude();
                if (!(params.containsKey(key) && params.get(key).contains(value))) {
                    if (value != null && (excludeExpr == null || !(boolean) excludeExpr.eval(evaluationContext))) {
                        params.put(key, userAnswers.get(key));
                    }
                }
            }
        }
        return params;
    }

    private EvaluationContext getEvaluationContextWithUserInputInstance() {
        Map<String, String> userQueryValues = getUserQueryValues(false);
        String refId = queryDatum.getDataId();
        ExternalDataInstance userInputInstance = VirtualInstances.buildSearchInputInstance(
                refId, userQueryValues);
        return evaluationContext.spawnWithCleanLifecycle(
                ImmutableMap.of(
                        userInputInstance.getInstanceId(), userInputInstance,
                        // Temporary method to make the 'search-input' instance available using the legacy ID
                        // Technically this instance elements should get renamed to match the instance ID, but
                        // it's OK here since the other instance is always going to be in the eval context.
                        "search-input", userInputInstance
                )
        );
    }

    public static String evalXpathExpression(XPathExpression expr,
            EvaluationContext evaluationContext) {
        return FunctionUtils.toString(expr.eval(evaluationContext));
    }

    public void populateItemSetChoices(QueryPrompt queryPrompt) {
        ItemSetUtils.populateDynamicChoices(queryPrompt.getItemsetBinding(),
                getEvaluationContextWithUserInputInstance());
    }

    public Map<String, String> getUserQueryValues(boolean includeNulls) {
        Map<String, String> values = new HashMap<>();
        OrderedHashtable<String, QueryPrompt> queryPrompts = queryDatum.getUserQueryPrompts();
        for (Enumeration en = queryPrompts.keys(); en.hasMoreElements(); ) {
            String promptId = (String)en.nextElement();
            if (isPromptSupported(queryPrompts.get(promptId))) {
                String answer = userAnswers.get(promptId);
                if (includeNulls || answer != null) {
                    values.put(promptId, answer);
                }
            }
        }
        return values;
    }

    // loops over query prompts and validates selection until all selections are valid
    public void refreshItemSetChoices() {
        OrderedHashtable<String, QueryPrompt> userInputDisplays = getNeededUserInputDisplays();
        if (userInputDisplays.size() == 0) {
            return;
        }

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
                if (queryPrompt.isSelect()) {
                    String answer = userAnswers.get(promptId);
                    populateItemSetChoices(queryPrompt);
                    String[] selectedChoices = extractMultipleChoices(answer);
                    ArrayList<String> validSelectedChoices = new ArrayList<>();
                    for (String selectedChoice : selectedChoices) {
                        if (checkForValidSelectValue(queryPrompt.getItemsetBinding(), selectedChoice)) {
                            validSelectedChoices.add(selectedChoice);
                        } else {
                            dirty = true;
                        }
                    }
                    if (validSelectedChoices.size() > 0) {
                        userAnswers.put(promptId,
                                String.join(RemoteQuerySessionManager.ANSWER_DELIMITER, validSelectedChoices));
                    } else {
                        // no value
                        userAnswers.remove(promptId);
                    }
                }
            }
            index++;
        }
    }

    public boolean isPromptSupported(QueryPrompt queryPrompt) {
        return queryPrompt.getInput() == null || supportedPrompts.indexOf(queryPrompt.getInput()) != -1;
    }

    // checks if @param{value} is one of the select choices give in @param{items}
    private boolean checkForValidSelectValue(ItemsetBinding itemsetBinding, String value) {
        // blank is always a valid choice
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        return ItemSetUtils.getIndexOf(itemsetBinding, value) != -1;
    }

    public boolean doDefaultSearch() {
        return queryDatum.doDefaultSearch();
    }

    // Converts a string containing space separated list of choices
    // into a string array of individual choices
    public static String[] extractMultipleChoices(String answer) {
        if (answer == null) {
            return new String[]{};
        }
        return answer.split(ANSWER_DELIMITER);
    }

    public RemoteQueryDatum getQueryDatum() {
        return queryDatum;
    }
}
