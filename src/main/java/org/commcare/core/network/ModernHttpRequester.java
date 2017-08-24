package org.commcare.core.network;

import org.commcare.core.interfaces.HttpResponseProcessor;
import org.commcare.core.interfaces.ResponseStreamAccessor;
import org.commcare.core.network.bitcache.BitCache;
import org.commcare.core.network.bitcache.BitCacheFactory;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
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

    private final HTTPMethod method;
    private final BitCacheFactory.CacheDirSetup cacheDirSetup;
    private final RequestBody requestBody;
    private final List<MultipartBody.Part> parts;
    private final HttpResponseProcessor responseProcessor;
    protected final String url;
    protected final HashMap<String, String> params;
    protected final HashMap<String, String> headers;
    private Response<ResponseBody> response;
    private CommCareNetworkService commCareNetworkService;

    @Nullable
    private Call currentCall;

    /**
     * responseProcessor Can be null if you want to process the response yourself. Please use makeRequest() instead of makeRequestAndProcess()
     * to make a request in case of responseProcessor being null.
     */
    public ModernHttpRequester(BitCacheFactory.CacheDirSetup cacheDirSetup,
                               String url, HashMap<String, String> params, HashMap<String, String> headers,
                               @Nullable RequestBody requestBody, @Nullable List<MultipartBody.Part> parts,
                               CommCareNetworkService commCareNetworkService,
                               HTTPMethod method,
                               @Nullable HttpResponseProcessor responseProcessor) {
        this.cacheDirSetup = cacheDirSetup;
        this.params = params;
        this.headers = headers;
        this.url = url;
        this.method = method;
        this.requestBody = requestBody;
        this.parts = parts;
        this.commCareNetworkService = commCareNetworkService;
        this.responseProcessor = responseProcessor;
    }

    /**
     * Executes and process the Request using the ResponseProcessor.
     */
    @Nullable
    public void makeRequestAndProcess() {
        if (responseProcessor == null) {
            throw new IllegalStateException("Please call makeRequest since responseProcessor is null");
        }
        try {
            response = makeRequest();
            processResponse(responseProcessor, response.code(), this);
        } catch (IOException | AuthenticationInterceptor.PlainTextPasswordException e) {
            e.printStackTrace();
            responseProcessor.handleException(e);
        }
    }

    /**
     * Executes the HTTP Request. Can be called directly to bypass response processor.
     *
     * @return Response from the HTTP call
     * @throws IOException
     */
    public Response<ResponseBody> makeRequest() throws IOException {
        switch (method) {
            case POST:
                currentCall = commCareNetworkService.makePostRequest(url, params, addPostHeaders(requestBody), requestBody);
                break;
            case MULTIPART_POST:
                currentCall = commCareNetworkService.makeMultipartPostRequest(url, params, headers, parts);
                break;
            case GET:
                currentCall = commCareNetworkService.makeGetRequest(url, params, headers);
                break;
            default:
                throw new IllegalArgumentException("Invalid HTTPMethod " + method.toString());
        }
        return currentCall.execute();
    }


    private Map<String, String> addPostHeaders(RequestBody postBody) {
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
                responseProcessor.handleException(e);
                return;
            }
            responseProcessor.processSuccess(responseCode, responseStream);
        } else if (responseCode >= 400 && responseCode < 500) {
            responseProcessor.processClientError(responseCode);
        } else if (responseCode >= 500 && responseCode < 600) {
            responseProcessor.processServerError(responseCode);
        } else {
            responseProcessor.processOther(responseCode);
        }
    }

    /**
     * Only gets called if response processor is supplied
     *
     * @return Input Stream from cache
     * @throws IOException
     */
    @Override
    public InputStream getResponseStream() throws IOException {
        InputStream inputStream = response.body().byteStream();
        BitCache cache = BitCacheFactory.getCache(cacheDirSetup, getContentLength(response));
        cache.initializeCache();
        OutputStream cacheOut = cache.getCacheStream();
        StreamsUtil.writeFromInputToOutputNew(inputStream, cacheOut);
        return cache.retrieveCache();
    }

    public static RequestBody getPostBody(HashMap<String, String> inputs) {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> param : inputs.entrySet()) {
            formBodyBuilder.add(param.getKey(), param.getValue());
        }
        return formBodyBuilder.build();
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

    public void cancelRequest() {
        if (currentCall != null) {
            currentCall.cancel();
        }
    }
}
