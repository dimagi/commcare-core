package org.commcare.core.network;

import org.commcare.core.interfaces.HttpResponseProcessor;
import org.commcare.core.interfaces.ResponseStreamAccessor;
import org.commcare.core.network.bitcache.BitCache;
import org.commcare.core.network.bitcache.BitCacheFactory;
import org.commcare.modern.util.Pair;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Make http get/post requests with query params encoded in get url or post
 * body. Delegates response to appropriate response processor callback
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class ModernHttpRequester implements ResponseStreamAccessor {
    /**
     * How long to wait when opening network connection in milliseconds
     */
    public static final int CONNECTION_TIMEOUT = (int)TimeUnit.MINUTES.toMillis(2);

    /**
     * How long to wait when receiving data (in milliseconds)
     */
    public static final int CONNECTION_SO_TIMEOUT = (int)TimeUnit.MINUTES.toMillis(1);

    private final boolean isPostRequest;
    private final BitCacheFactory.CacheDirSetup cacheDirSetup;
    private HttpResponseProcessor responseProcessor;
    protected final URL url;
    protected final HashMap<String, String> params;
    private String credential = null;
    private Response<ResponseBody> response;

    // for an already-logged-in user
    public ModernHttpRequester(BitCacheFactory.CacheDirSetup cacheDirSetup,
                               URL url, HashMap<String, String> params,
                               User user, String domain, boolean isAuthenticatedRequest,
                               boolean isPostRequest) {
        this.isPostRequest = isPostRequest;
        this.cacheDirSetup = cacheDirSetup;
        this.params = params;
        this.url = url;

        setupAuthentication(isAuthenticatedRequest, user, domain);
    }

    // for a not-yet-logged-in user
    public ModernHttpRequester(BitCacheFactory.CacheDirSetup cacheDirSetup,
                               URL url, HashMap<String, String> params,
                               Pair<String, String> usernameAndPasswordToAuthWith, boolean isPostRequest) {
        this.isPostRequest = isPostRequest;
        this.cacheDirSetup = cacheDirSetup;
        this.params = params;
        this.url = url;

        setupAuthentication(usernameAndPasswordToAuthWith.first, usernameAndPasswordToAuthWith.second, null);
    }

    private void setupAuthentication(boolean isAuth, User user, String domain) {
        if (isAuth) {
            final String username;
            if (domain != null) {
                username = user.getUsername() + "@" + domain;
            } else {
                username = user.getUsername();
            }
            final String password = user.getCachedPwd();
            setupAuthentication(username, password, user);
        }
    }

    private void setupAuthentication(final String username, final String password, User user) {
        if (username == null || password == null ||
                (user != null && User.TYPE_DEMO.equals(user.getUserType()))) {
            String message =
                    "Trying to make authenticated http request without proper credentials";
            throw new RuntimeException(message);
        } else if (!"https".equals(url.getProtocol())) {
            throw new PlainTextPasswordException();
        } else {
            credential = getCredentials(username, password);
        }
    }

    public void setResponseProcessor(HttpResponseProcessor responseProcessor) {
        this.responseProcessor = responseProcessor;
    }

    public static class PlainTextPasswordException extends RuntimeException {
    }

    public void request() {
        try {
            CommCareNetworkService commCareNetworkService = CommCareNetworkServiceGenerator.createCommCareNetworkService(credential);
            if (isPostRequest) {
                RequestBody postBody = getPostBody();
                response = commCareNetworkService.makePostRequest(url.toString(), new HashMap(), getPostHeaders(postBody), postBody).execute();
            } else {
                response = commCareNetworkService.makeGetRequest(url.toString(), params, new HashMap()).execute();
            }
            processResponse(responseProcessor, response.code(), this);
        } catch (IOException e) {
            e.printStackTrace();
            responseProcessor.handleIOException(e);
        }
    }

    private RequestBody getPostBody() {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            formBodyBuilder.add(param.getKey(), param.getValue());
        }
        return formBodyBuilder.build();
    }

    private Map<String, String> getPostHeaders(RequestBody postBody) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        try {
            headers.put("Content-Length", postBody.contentLength() + "");
        } catch (IOException e) {
            // Do we care ?
            e.printStackTrace();
        }
        return headers;
    }


    public static void processResponse(HttpResponseProcessor responseProcessor,
                                       int responseCode,
                                       ResponseStreamAccessor streamAccessor) {
        if (responseCode >= 200 && responseCode < 300) {
            InputStream responseStream;
            try {
                responseStream = streamAccessor.getResponseStream();
            } catch (IOException e) {
                responseProcessor.handleIOException(e);
                return;
            }
            responseProcessor.processSuccess(responseCode, responseStream);
        } else if (responseCode >= 300 && responseCode < 400) {
            responseProcessor.processRedirection(responseCode);
        } else if (responseCode >= 400 && responseCode < 500) {
            responseProcessor.processClientError(responseCode);
        } else if (responseCode >= 500 && responseCode < 600) {
            responseProcessor.processServerError(responseCode);
        } else {
            responseProcessor.processOther(responseCode);
        }
    }

    @Override
    public InputStream getResponseStream() throws IOException {
        InputStream inputStream = response.body().byteStream();
        BitCache cache = BitCacheFactory.getCache(cacheDirSetup, getContentLength(response));
        cache.initializeCache();
        OutputStream cacheOut = cache.getCacheStream();
        StreamsUtil.writeFromInputToOutputNew(inputStream, cacheOut);
        return cache.retrieveCache();
    }

    private static String getCredentials(String username, String password) {
        if (username == null || password == null) {
            return null;
        } else {
            return okhttp3.Credentials.basic(username, password);
        }
    }

    public static long getContentLength(Response response) {
        long contentLength = -1;
        String length = getFirstHeader(response, "Content-Length");
        try {
            contentLength = Long.parseLong(length);
        } catch (Exception e) {
            //Whatever.
        }
        return contentLength;
    }

    public static String getFirstHeader(Response response, String headerName) {
        List<String> headers = response.headers().values(headerName);
        if (headers.size() > 0) {
            return headers.get(0);
        }
        return null;
    }
}
