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
 * Screen to make a sync request to HQ after a case claim. Unlike other all other screens,
 * SyncScreen does not take input - it simply processes the PostRequest object, makes the request,
 * and displays the result (if necessary)
 */
public class SyncScreen extends Screen {

    protected SessionWrapper sessionWrapper;
    private String username;
    private String password;


    public SyncScreen(String username, String password) {
        super();
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
            Response response = makeSyncRequest(syncPost);
            if (!response.isSuccessful()) {
                throw new CommCareSessionException(
                        String.format("Sync request failed with code %s and message %s.",
                                response.code(),
                                response.message()
                        )
                );
            }
        } else {
            // expected a sync entry; clear session and show vague 'session error' message to user
            throw new CommCareSessionException("Initialized sync request while not on sync screen");
        }
    }

    private static String buildUrl(String baseUrl) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
        return urlBuilder.build().toString();
    }

    private static MultipartBody buildPostBody(Hashtable<String, String> params) {
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        // Add buffer param since this is necessary for some reason
        requestBodyBuilder.addFormDataPart("buffer", "buffer");
        for (String key: params.keySet()) {
            requestBodyBuilder.addFormDataPart(key, params.get(key));
        }
        return requestBodyBuilder.build();
    }

    private Response makeSyncRequest(PostRequest syncPost) throws CommCareSessionException {
        Hashtable<String, String> params = syncPost.getEvaluatedParams(sessionWrapper.getEvaluationContext());
        String url = buildUrl(syncPost.getUrl().toString());
        MultipartBody postBody = buildPostBody(params);
        String credential = Credentials.basic(username, password);

        Request request = new Request.Builder()
                .url(url)
                .method("POST", RequestBody.create(null, new byte[0]))
                .header("Authorization", credential)
                .post(postBody)
                .build();
        try {
            OkHttpClient client = new OkHttpClient();
            return client.newCall(request).execute();
        } catch (IOException e) {
            throw new CommCareSessionException("Exception while making sync request", e);
        }
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
