package org.commcare.util.screen;

import com.google.common.collect.Multimap;

import org.commcare.cases.util.CaseDBUtils;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.parse.ParseUtils;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.PostRequest;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.User;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SessionUtils {
    public SessionUtils() {}

    public void restoreUserToSandbox(UserSandbox sandbox,
                                            SessionWrapper session,
                                            CommCarePlatform platform,
                                            String username,
                                            final String password,
                                            PrintStream printStream) {
        String urlStateParams = "";

        boolean failed = true;

        boolean incremental = false;

        if (sandbox.getLoggedInUser() != null) {
            String syncToken = sandbox.getSyncToken();
            String caseStateHash = CaseDBUtils.computeCaseDbHash(sandbox.getCaseStorage());

            urlStateParams = String.format("&since=%s&state=ccsh:%s", syncToken, caseStateHash);
            incremental = true;

            printStream.println(String.format(
                    "\nIncremental sync requested. \nSync Token: %s\nState Hash: %s",
                    syncToken, caseStateHash));
        }

        PropertyManager propertyManager = platform.getPropertyManager();

        //fetch the restore data and set credentials
        String otaFreshRestoreUrl = propertyManager.getSingularProperty("ota-restore-url") +
                "?version=2.0";

        String otaSyncUrl = otaFreshRestoreUrl + urlStateParams;

        String domain = propertyManager.getSingularProperty("cc_user_domain");
        final String qualifiedUsername;
        if (username.contains("@")) {
            qualifiedUsername = username;
        } else {
            qualifiedUsername = username + "@" + domain;
        }

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(qualifiedUsername, password.toCharArray());
            }
        });

        //Go get our sandbox!
        try {
            printStream.println("GET: " + otaSyncUrl);
            URL url = new URL(otaSyncUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            if (conn.getResponseCode() == 412) {
                printStream.println("Server Response 412 - The user sandbox is not consistent with " +
                        "the server's data. \n\nThis is expected if you have changed cases locally, " +
                        "since data is not sent to the server for updates. \n\nServer response cannot be restored," +
                        " you will need to restart the user's session to get new data.");
            } else if (conn.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                printStream.println("\nInvalid username or password!");
            } else if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {

                printStream.println("Restoring user " + username + " to domain " + domain);

                ParseUtils.parseIntoSandbox(new BufferedInputStream(conn.getInputStream()), sandbox);

                printStream.println("User data processed, new state token: " + sandbox.getSyncToken());
                failed = false;
            } else {
                printStream.println("Unclear/Unexpected server response code: " + conn.getResponseCode());
            }
        } catch (InvalidStructureException | IOException
                | XmlPullParserException | UnfullfilledRequirementsException e) {
            e.printStackTrace();
        }

        if (failed) {
            if (!incremental) {
                System.exit(-1);
            }
        } else {
            //Initialize our User
            for (IStorageIterator<User> iterator = sandbox.getUserStorage().iterate(); iterator.hasMore(); ) {
                User u = iterator.nextRecord();
                if (username.equalsIgnoreCase(u.getUsername())) {
                    sandbox.setLoggedInUser(u);
                }
            }
        }

        if (session != null) {
            // old session data is now no longer valid
            session.clearVolatiles();
        }
    }

    public int doPostRequest(
            PostRequest syncPost,
            SessionWrapper session,
            final String username,
            final String password,
            PrintStream printStream
    ) throws IOException {
        String url = buildUrl(syncPost.getUrl().toString());
        Multimap<String, String> params = syncPost.getEvaluatedParams(session.getEvaluationContext(), true);
        printStream.println(String.format("Syncing with url %s and parameters %s", url, params));
        MultipartBody postBody = buildPostBody(params);
        String credential = Credentials.basic(username, password);
        Request request = new Request.Builder()
                .url(url)
                .method("POST", RequestBody.create(new byte[0]))
                .header("Authorization", credential)
                .post(postBody)
                .build();
        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String message = String.format("Post request failed with response code %s and message %s",
                    response.code(), response.body());
            printStream.println(message);
            printStream.println("Press 'enter' to retry.");
            throw new IOException(message);
        }
        return response.code();
    }

    private static String buildUrl(String baseUrl) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
        return urlBuilder.build().toString();
    }

    private static MultipartBody buildPostBody(Multimap<String, String> params) {
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        // Add buffer param since this is necessary for some reason
        requestBodyBuilder.addFormDataPart("buffer", "buffer");
        params.forEach(requestBodyBuilder::addFormDataPart);
        return requestBodyBuilder.build();
    }
}
