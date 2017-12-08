package org.commcare.util.screen;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.CommCareSession;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.DisplayUnit;
import org.javarosa.core.model.instance.ExternalDataInstance;

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

    protected RemoteQuerySessionManager remoteQuerySessionManager;
    Hashtable<String, DisplayUnit> userInputDisplays;
    SessionWrapper sessionWrapper;
    String[] fields;
    String mTitle;
    String currentMessage;

    String username;
    String password;

    public QueryScreen(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void init(SessionWrapper sessionWrapper) throws CommCareSessionException {
        this.sessionWrapper = sessionWrapper;
        remoteQuerySessionManager =
                RemoteQuerySessionManager.buildQuerySessionManager(sessionWrapper,
                        sessionWrapper.getEvaluationContext());
        userInputDisplays = remoteQuerySessionManager.getNeededUserInputDisplays();

        int count = 0;
        fields = new String[userInputDisplays.keySet().size()];
        for (Map.Entry<String, DisplayUnit> displayEntry : userInputDisplays.entrySet()) {
            fields[count] = displayEntry.getValue().getText().evaluate(sessionWrapper.getEvaluationContext());
        }
        mTitle = "Case Claim";

    }


    public InputStream makeQueryRequestReturnStream() {
        OkHttpClient client = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getBaseUrl().toString()).newBuilder();
        for (String key: getQueryParams().keySet()) {
            urlBuilder.addQueryParameter(key, getQueryParams().get(key));
        }
        String url = urlBuilder.build().toString();
        String credential = Credentials.basic(username, password);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .build();
        try {
            Response response = client.newCall(request).execute();
            return response.body().byteStream();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean processResponse(InputStream responseData) {
        if (responseData == null) {
            currentMessage = "Query failed";
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
            return false;
        }
    }

    private boolean isResponseEmpty(ExternalDataInstance instance) {
        return !instance.getRoot().hasChildren();
    }

    public void answerPrompts(Hashtable<String, String> answers) {
        for(String key: answers.keySet()){
            remoteQuerySessionManager.answerUserPrompt(key, answers.get(key));
        }
    }

    public URL getBaseUrl(){
        return remoteQuerySessionManager.getBaseUrl();
    }

    public Hashtable<String, String> getQueryParams(){
        return remoteQuerySessionManager.getRawQueryParams();
    }

    public String getScreenTitle() {
        return mTitle;
    }

    @Override
    public void prompt(PrintStream out) {
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
        return processResponse(response);
    }

    public Hashtable<String, DisplayUnit> getUserInputDisplays(){
        return userInputDisplays;
    }

    public String getCurrentMessage(){
        return currentMessage;
    }
}
