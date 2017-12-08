package org.commcare.util.screen;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.PostRequest;
import org.commcare.suite.model.RemoteRequestEntry;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Screen to make a sync request to HQ after a case claim
 */
public class SyncScreen extends Screen {

    private String asUser;
    private String builtQuery;
    private String url;
    protected SessionWrapper sessionWrapper;
    private String username;
    private String password;


    public SyncScreen(String asUser, String username, String password) {
        super();
        this.asUser = asUser;
        this.username = username;
        this.password = password;
    }

    @Override
    public void init (SessionWrapper sessionWrapper) throws CommCareSessionException {
        this.sessionWrapper = sessionWrapper;
        String command = sessionWrapper.getCommand();
        Entry commandEntry = sessionWrapper.getPlatform().getEntry(command);
        if (commandEntry instanceof RemoteRequestEntry) {
            PostRequest syncPost = ((RemoteRequestEntry)commandEntry).getPostRequest();
            makeSyncRequest(syncPost);
        } else {
            // expected a sync entry; clear session and show vague 'session error' message to user
            throw new RuntimeException("Initialized sync request while not on sync screen");
        }
    }

    private void makeSyncRequest(PostRequest syncPost) {
        setUrl(syncPost.getUrl().toString());
        Hashtable<String, String> params = syncPost.getEvaluatedParams(sessionWrapper.getEvaluationContext());
        OkHttpClient client = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(syncPost.getUrl().toString()).newBuilder();
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("somParam", "someValue");

        requestBodyBuilder.addFormDataPart("buffer", "buffer");
        for (String key: params.keySet()) {
            requestBodyBuilder.addFormDataPart(key, params.get(key));
        }
        String url = urlBuilder.build().toString();
        String credential = Credentials.basic(username, password);

        Request request = new Request.Builder()
                .url(url)
                .method("POST", RequestBody.create(null, new byte[0]))
                .header("Authorization", credential)
                .post(requestBodyBuilder.build())
                .build();
        try {
            Response response = client.newCall(request).execute();
            System.out.println("Response: " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getBuiltQuery() {
        return builtQuery;
    }

    public void setBuiltQuery(String builtQuery) {
        this.builtQuery = builtQuery;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void prompt(PrintStream printStream) throws CommCareSessionException {

    }

    @Override
    public boolean handleInputAndUpdateSession(CommCareSession commCareSession, String s) throws CommCareSessionException {
        return false;
    }

    @Override
    public String[] getOptions() {
        return new String[0];
    }
}
