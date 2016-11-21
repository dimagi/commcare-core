package org.commcare.util.screen;

import org.apache.http.client.utils.URIBuilder;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.CommCareSession;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.DisplayUnit;
import org.javarosa.core.model.instance.ExternalDataInstance;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

/**
 * Screen that displays user configurable entry texts and makes
 * a case query to the server with these fields.
 *
 * @author wspride
 */
public class QueryScreen extends Screen {

    protected RemoteQuerySessionManager remoteQuerySessionManager;
    private Hashtable<String, DisplayUnit> userInputDisplays;
    private SessionWrapper sessionWrapper;
    private String[] fields;
    private String mTitle;
    private String currentMessage;

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
        try {
            URL urlObject = getBaseUrl();
            Hashtable<String, String> params = getQueryParams();
            URIBuilder uriBuilder = new URIBuilder(urlObject.toString());
            for(String key: params.keySet()) {
                uriBuilder.addParameter(key, params.get(key));
            }
            urlObject = uriBuilder.build().toURL();
            HttpURLConnection con = null;
            con = (HttpURLConnection) urlObject.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            return con.getInputStream();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }


    public boolean processSuccess(InputStream responseData) {
        Pair<ExternalDataInstance, String> instanceOrError = remoteQuerySessionManager.buildExternalDataInstance(responseData);
        if (instanceOrError.first == null) {
            currentMessage = "Query response format error: " + instanceOrError.second;
            return false;
        } else if (isResponseEmpty(instanceOrError.first)) {
            currentMessage = "Query response was empty";
            return false;
        } else {
            sessionWrapper.setQueryDatum(instanceOrError.first);
            return true;
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
        return !processSuccess(response);
    }

    public Hashtable<String, DisplayUnit> getUserInputDisplays(){
        return userInputDisplays;
    }

    public String getCurrentMessage(){
        return currentMessage;
    }
}
