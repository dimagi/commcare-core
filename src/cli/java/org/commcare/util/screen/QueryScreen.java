package org.commcare.util.screen;

import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_ADDRESS;
import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_DATE;
import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_DATERANGE;
import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_SELECT;
import static org.commcare.suite.model.QueryPrompt.INPUT_TYPE_SELECT1;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

import org.commcare.cases.util.StringUtils;
import org.commcare.core.encryption.CryptUtil;
import org.commcare.core.interfaces.VirtualDataInstanceStorage;
import org.commcare.data.xml.VirtualInstances;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.CommCareSession;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.suite.model.RemoteQueryDatum;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
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
    private String currentMessage;

    private String domainedUsername;
    private String password;

    private PrintStream out;
    private VirtualDataInstanceStorage instanceStorage;
    private SessionUtils sessionUtils;

    private boolean defaultSearch;

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

        try {
            mTitle = Localization.get("case.search.title");
        } catch (NoLocalizedTextException nlte) {
            mTitle = "Case Claim";
        }
    }

    // Formplayer List of Supported prompts
    private ArrayList<String> getSupportedPrompts() {
        ArrayList<String> supportedPrompts = new ArrayList<>();
        supportedPrompts.add(INPUT_TYPE_SELECT1);
        supportedPrompts.add(INPUT_TYPE_SELECT);
        supportedPrompts.add(INPUT_TYPE_DATE);
        supportedPrompts.add(INPUT_TYPE_DATERANGE);
        supportedPrompts.add(INPUT_TYPE_ADDRESS);
        return supportedPrompts;
    }

    public Multimap<String, String> getRequestData(boolean skipDefaultPromptValues) {
        ImmutableListMultimap.Builder<String, String> dataBuilder = ImmutableListMultimap.builder();
        Multimap<String, String> queryParams = getQueryParams(skipDefaultPromptValues);
        for (String key : queryParams.keySet()) {
            QueryPrompt prompt = userInputDisplays.get(key);
            for (String value : queryParams.get(key)) {
                if (prompt != null) {
                    String[] choices = RemoteQuerySessionManager.extractMultipleChoices(value);
                    for (String choice : choices) {
                        dataBuilder.put(key, choice);
                    }
                } else {
                    dataBuilder.put(key, value);
                }
            }
        }
        return dataBuilder.build();
    }

    public Pair<ExternalDataInstance, String> processResponse(InputStream responseData, URL url,
            Multimap<String, String> requestData) {
        if (responseData == null) {
            currentMessage = "Query result null.";
            return new Pair<>(null, currentMessage);
        }
        Pair<ExternalDataInstance, String> instanceOrError;
        try {
            String instanceID = getQueryDatum().getDataId();
            TreeElement root = ExternalDataInstance.parseExternalTree(responseData, instanceID);
            ExternalDataInstanceSource instanceSource = ExternalDataInstanceSource.buildRemote(
                    instanceID, root, getQueryDatum().useCaseTemplate(), url.toString(), requestData);
            ExternalDataInstance instance = instanceSource.toInstance();
            instanceOrError = new Pair<>(instance, "");
        } catch (InvalidStructureException | IOException
                | XmlPullParserException | UnfullfilledRequirementsException e) {
            instanceOrError = new Pair<>(null, e.getMessage());
        }

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
        String instanceID = VirtualInstances.makeSearchInputInstanceID(refId);
        Map<String, String> userQueryValues = remoteQuerySessionManager.getUserQueryValues(false);
        String key = getInstanceKey(instanceID, userQueryValues);
        if (instanceStorage.contains(key)) {
            return instanceStorage.read(key, instanceID);
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
            QueryPrompt queryPrompt = userInputDisplays.get(key);
            String answer = answers.get(key);

            // If select question, we should have got an index as the answer which should
            // be converted to the corresponding value
            if (queryPrompt.isSelect() && !StringUtils.isEmpty(answer)) {
                Vector<SelectChoice> selectChoices = queryPrompt.getItemsetBinding().getChoices();
                String[] indicesOfSelectedChoices = RemoteQuerySessionManager.extractMultipleChoices(answer);
                ArrayList<String> selectedChoices = new ArrayList<>(indicesOfSelectedChoices.length);
                for (int i = 0; i < indicesOfSelectedChoices.length; i++) {
                    if (indicesOfSelectedChoices[i].isEmpty()) {
                        selectedChoices.add("");
                    } else {
                        int choiceIndex = Integer.parseInt(indicesOfSelectedChoices[i]);
                        if (choiceIndex < selectChoices.size() && choiceIndex > -1) {
                            selectedChoices.add(selectChoices.get(choiceIndex).getValue());
                        }
                    }
                }
                answer = String.join(RemoteQuerySessionManager.ANSWER_DELIMITER, selectedChoices);
            }
            remoteQuerySessionManager.answerUserPrompt(key, answer);
        }
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
    protected Multimap<String, String> getQueryParams(boolean skipDefaultPromptValues) {
        return remoteQuerySessionManager.getRawQueryParams(skipDefaultPromptValues);
    }

    public String getScreenTitle() {
        return mTitle;
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
        Multimap<String, String> requestData = getRequestData(false);
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

    public boolean doDefaultSearch() {
        return remoteQuerySessionManager.doDefaultSearch();
    }

    public RemoteQueryDatum getQueryDatum() {
        return remoteQuerySessionManager.getQueryDatum();
    }

    @Override
    public String toString() {
        return "QueryScreen[" + mTitle + "]";
    }
}
