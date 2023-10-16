package org.commcare.util.screen;

import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_ADDRESS;
import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_CHECKBOX;
import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_DATE;
import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_DATERANGE;
import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_SELECT;
import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_SELECT1;

import com.google.common.collect.Multimap;

import org.commcare.cases.util.StringUtils;
import org.commcare.core.encryption.CryptUtil;
import org.commcare.core.interfaces.VirtualDataInstanceStorage;
import org.commcare.data.xml.VirtualInstances;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.CommCareSession;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.QueryPrompt;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.core.util.OrderedHashtable;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import datadog.trace.api.Trace;

/**
 * Screen that displays user configurable entry texts and makes
 * a case query to the server with these fields.
 *
 * @author wspride
 */
public class QueryScreen extends Screen {

    private RemoteQuerySessionManager remoteQuerySessionManager;
    protected OrderedHashtable<String, QueryPrompt> userInputDisplays;
    private SessionWrapper sessionWrapper;
    private String[] fields;
    private String mTitle;
    private String description;
    private String currentMessage;

    private String domainedUsername;
    private String password;

    private PrintStream out;
    private VirtualDataInstanceStorage instanceStorage;
    private SessionUtils sessionUtils;

    private boolean defaultSearch;

    private boolean dynamicSearch;

    public QueryScreen(String domainedUsername, String password, PrintStream out,
            VirtualDataInstanceStorage instanceStorage, SessionUtils sessionUtils) {
        this.domainedUsername = domainedUsername;
        this.password = password;
        this.out = out;
        this.instanceStorage = instanceStorage;
        this.sessionUtils = sessionUtils;
    }

    @Override
    public void init(SessionWrapper sessionWrapper) throws CommCareSessionException {
        this.sessionWrapper = sessionWrapper;
        remoteQuerySessionManager =
                RemoteQuerySessionManager.buildQuerySessionManager(sessionWrapper,
                        sessionWrapper.getEvaluationContext(), getSupportedPrompts());

        if (remoteQuerySessionManager == null) {
            throw new CommCareSessionException(String.format("QueryManager for case " +
                    "claim screen with id %s cannot be null.", sessionWrapper.getNeededData()));
        }
        userInputDisplays = remoteQuerySessionManager.getNeededUserInputDisplays();

        int count = 0;
        fields = new String[userInputDisplays.keySet().size()];
        for (Map.Entry<String, QueryPrompt> queryPromptEntry : userInputDisplays.entrySet()) {
            fields[count] = queryPromptEntry.getValue().getDisplay().getText().evaluate(
                    sessionWrapper.getEvaluationContext());
            count++;
        }

        mTitle = getTitleLocaleString();
        description = getDescriptionLocaleString();
    }

    private String getTitleLocaleString() {
        try {
            mTitle = getQueryDatum().getTitleText().evaluate();
        } catch (NoLocalizedTextException | NullPointerException e) {
            mTitle = getTitleLocaleStringLegacy();
        }
        return mTitle;
    }

    private String getDescriptionLocaleString() {
        try {
            description = getQueryDatum().getDescriptionText().evaluate();
        } catch (NoLocalizedTextException | NullPointerException e) {
            description = "";
        }
        return description;
    }

    private String getTitleLocaleStringLegacy() {
        try {
            mTitle = Localization.get("case.search.title");
        } catch (NoLocalizedTextException | NullPointerException e) {
            mTitle = "Case Claim";
        }
        return mTitle;
    }

    // Formplayer List of Supported prompts
    private ArrayList<String> getSupportedPrompts() {
        ArrayList<String> supportedPrompts = new ArrayList<>();
        supportedPrompts.add(INPUT_TYPE_SELECT1);
        supportedPrompts.add(INPUT_TYPE_SELECT);
        supportedPrompts.add(INPUT_TYPE_DATE);
        supportedPrompts.add(INPUT_TYPE_DATERANGE);
        supportedPrompts.add(INPUT_TYPE_CHECKBOX);
        supportedPrompts.add(INPUT_TYPE_ADDRESS);
        return supportedPrompts;
    }

    public Pair<ExternalDataInstance, String> processResponse(InputStream responseData, URL url,
            Multimap<String, String> requestData) {
        if (responseData == null) {
            currentMessage = "Query result null.";
            return new Pair<>(null, currentMessage);
        }
        Pair<ExternalDataInstance, String> instanceOrError = remoteQuerySessionManager.buildExternalDataInstance(
                responseData, url.toString(), requestData);
        if (instanceOrError.first == null) {
            currentMessage = "Query response format error: " + instanceOrError.second;
        }
        return instanceOrError;
    }

    public void updateSession(ExternalDataInstance dataInstance) {
        if (dataInstance != null) {
            ExternalDataInstance userInputInstance = getUserInputInstance();
            sessionWrapper.setQueryDatum(dataInstance, userInputInstance);
        }
    }

    private ExternalDataInstance getUserInputInstance() {
        String refId = getQueryDatum().getDataId();
        String instanceId = VirtualInstances.makeSearchInputInstanceID(refId);
        Map<String, String> userQueryValues = remoteQuerySessionManager.getUserQueryValues(false);
        String key = getInstanceKey(instanceId, userQueryValues);
        if (instanceStorage.contains(key)) {
            return instanceStorage.read(key, instanceId, refId);
        }

        ExternalDataInstance userInputInstance = VirtualInstances.buildSearchInputInstance(
                refId, userQueryValues);
        instanceStorage.write(key, userInputInstance);
        // rebuild the instance with source
        return ExternalDataInstanceSource.buildVirtual(userInputInstance, key).toInstance();
    }

    private String getInstanceKey(String instanceId, Map<String, String> values) {
        StringBuilder builder = new StringBuilder(instanceId);
        builder.append("/");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            builder.append(entry.getKey()).append("=").append(entry.getValue()).append("|");
        }
        return CryptUtil.sha256(builder.toString());
    }

    public void answerPrompts(Hashtable<String, String> answers) {
        for (Enumeration en = userInputDisplays.keys(); en.hasMoreElements(); ) {
            String key = (String)en.nextElement();
            String answer = answers.get(key);
            remoteQuerySessionManager.answerUserPrompt(key, answer);
        }
        remoteQuerySessionManager.refreshInputDependentState();
    }

    public void refreshItemSetChoices() {
        remoteQuerySessionManager.refreshItemSetChoices();
    }

    public URL getBaseUrl() {
        return remoteQuerySessionManager.getBaseUrl();
    }

    /**
     * @param skipDefaultPromptValues don't apply the default value expressions for query prompts
     * @return filters to be applied to case search uri as query params
     */
    public Multimap<String, String> getQueryParams(boolean skipDefaultPromptValues) {
        return remoteQuerySessionManager.getRawQueryParams(skipDefaultPromptValues);
    }

    public String getScreenTitle() {
        return mTitle;
    }

    public String getDescriptionText() {
        return description;
    }

    public boolean getDynamicSearch() {
        return dynamicSearch;
    }

    @Override
    public boolean prompt(PrintStream out) {
        if (doDefaultSearch()) {
            return false;
        }
        out.println("Enter the search fields as a comma-separated list.");
        for (int i = 0; i < fields.length; i++) {
            out.println(i + ") " + fields[i]);
        }
        return true;
    }

    @Override
    public String[] getOptions() {
        return fields;
    }

    @Trace
    @Override
    public boolean handleInputAndUpdateSession(CommCareSession session, String input, boolean allowAutoLaunch,
            String[] selectedValues) {
        String[] answers = input.split(",");
        Hashtable<String, String> userAnswers = new Hashtable<>();
        int count = 0;
        for (Map.Entry<String, QueryPrompt> queryPromptEntry : userInputDisplays.entrySet()) {
            userAnswers.put(queryPromptEntry.getKey(), answers[count]);
            count++;
        }
        answerPrompts(userAnswers);
        URL url = getBaseUrl();
        Multimap<String, String> requestData = getQueryParams(false);
        InputStream response = sessionUtils.makeQueryRequest(url, requestData, domainedUsername, password);
        Pair<ExternalDataInstance, String> instanceOrError = processResponse(response, url, requestData);
        updateSession(instanceOrError.first);
        if (currentMessage != null) {
            out.println(currentMessage);
        }
        return instanceOrError.first != null;
    }

    public OrderedHashtable<String, QueryPrompt> getUserInputDisplays() {
        return userInputDisplays;
    }

    public String getCurrentMessage() {
        return currentMessage;
    }

    public Hashtable<String, String> getCurrentAnswers() {
        return remoteQuerySessionManager.getUserAnswers();
    }

    public Hashtable<String, String> getErrors() {
        return remoteQuerySessionManager.getErrors();
    }

    public Hashtable<String, Boolean> getRequiredPrompts() {
        return remoteQuerySessionManager.getRequiredPrompts();
    }

    public boolean doDefaultSearch() {
        return remoteQuerySessionManager.doDefaultSearch();
    }

    public RemoteQueryDatum getQueryDatum() {
        return remoteQuerySessionManager.getQueryDatum();
    }

    public SessionWrapper getSession() {
        return sessionWrapper;
    }

    @Override
    public String toString() {
        return "QueryScreen[" + mTitle + "]";
    }
}
