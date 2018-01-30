package org.commcare.util.screen;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.CommCareSession;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.DisplayUnit;
import org.javarosa.core.model.instance.ExternalDataInstance;

import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Screen that displays user configurable entry texts and makes
 * a case query to the server with these fields.
 *
 * @author wspride
 */
public class QueryScreen extends Screen {

    private RemoteQuerySessionManager remoteQuerySessionManager;
    private Hashtable<String, DisplayUnit> userInputDisplays;
    private SessionWrapper sessionWrapper;
    private String[] fields;
    private String mTitle;
    private String currentMessage;

    private String domainedUsername;
    private String password;

    private PrintStream out;

    public QueryScreen(String domainedUsername, String password, PrintStream out) {
        this.domainedUsername = domainedUsername;
        this.password = password;
        this.out = out;
    }

    @Override
    public void init(SessionWrapper sessionWrapper) throws CommCareSessionException {
        this.sessionWrapper = sessionWrapper;
        remoteQuerySessionManager =
                RemoteQuerySessionManager.buildQuerySessionManager(sessionWrapper,
                        sessionWrapper.getEvaluationContext());
        if (remoteQuerySessionManager == null) {
            throw new CommCareSessionException(String.format("QueryManager for case " +
                    "claim screen with id %s cannot be null.", sessionWrapper.getNeededData()));
        }
        userInputDisplays = remoteQuerySessionManager.getNeededUserInputDisplays();

        int count = 0;
        fields = new String[userInputDisplays.keySet().size()];
        for (Map.Entry<String, DisplayUnit> displayEntry : userInputDisplays.entrySet()) {
            fields[count] = displayEntry.getValue().getText().evaluate(sessionWrapper.getEvaluationContext());
        }
        mTitle = "Case Claim";

    }

    private static String buildUrl(String baseUrl, Hashtable<String, String> queryParams) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
        for (String key: queryParams.keySet()) {
            urlBuilder.addQueryParameter(key, queryParams.get(key));
        }
        return urlBuilder.build().toString();
    }


    private InputStream makeQueryRequestReturnStream() {
        String url = buildUrl(getBaseUrl().toString(), getQueryParams());
        String credential = Credentials.basic(domainedUsername, password);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .build();
        try {
            Response response = new OkHttpClient().newCall(request).execute();
            return response.body().byteStream();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean processResponse(InputStream responseData) {
        if (responseData == null) {
            currentMessage = "Query result null.";
            return false;
        }
        Pair<ExternalDataInstance, String> instanceOrError =
                remoteQuerySessionManager.buildExternalDataInstance(responseData);
        if (instanceOrError.first == null) {
            currentMessage = "Query response format error: " + instanceOrError.second;
            return false;
        } else if (isResponseEmpty(instanceOrError.first)) {
            currentMessage = "Query successful but returned no results.";

            return false;
        } else {
            sessionWrapper.setQueryDatum(instanceOrError.first);
            return true;
        }
    }

    private boolean isResponseEmpty(ExternalDataInstance instance) {
        return !instance.getRoot().hasChildren();
    }

    private void answerPrompts(Hashtable<String, String> answers) {
        for(String key: answers.keySet()){
            remoteQuerySessionManager.answerUserPrompt(key, answers.get(key));
        }
    }

    private URL getBaseUrl(){
        return remoteQuerySessionManager.getBaseUrl();
    }

    private Hashtable<String, String> getQueryParams(){
        return remoteQuerySessionManager.getRawQueryParams();
    }

    public String getScreenTitle() {
        return mTitle;
    }

    @Override
    public void prompt(PrintStream out) {
        out.println("Enter the search fields as a space separated list.");
        for (int i=0; i< fields.length; i++) {
            out.println(i + ") " + fields[i]);
        }
    }

    @Override
    public String[] getOptions() {
        return fields;
    }

    @Override
    public boolean handleInputAndUpdateSession(CommCareSession session, String input) {
        String[] answers = input.split(",");
        Hashtable<String, String> userAnswers = new Hashtable<>();
        int count = 0;
        for (Map.Entry<String, DisplayUnit> displayEntry : userInputDisplays.entrySet()) {
            userAnswers.put(displayEntry.getKey(), answers[count]);
            count ++;
        }
        answerPrompts(userAnswers);
        InputStream response = makeQueryRequestReturnStream();
        boolean refresh = processResponse(response);
        if (currentMessage != null) {
            out.println(currentMessage);
        }
        return refresh;
    }

    public Hashtable<String, DisplayUnit> getUserInputDisplays(){
        return userInputDisplays;
    }

    public String getCurrentMessage(){
        return currentMessage;
    }
}
