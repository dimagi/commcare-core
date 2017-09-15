package org.commcare.core.network;


import org.commcare.util.LogTypes;
import org.javarosa.core.services.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

/**
 * Provides an instance of CommCareNetworkService.
 * We have declared everything static in this class as we want to use the same objects (OkHttpClient, Retrofit, …) throughout the app
 * to just open one socket connection that handles all the request and responses.
 */

public class CommCareNetworkServiceGenerator {

    // Retrofit needs a base url to generate an instance but since our apis are fully dynamic it's not getting used.
    private static final String BASE_URL = "http://example.url/";

    private static Retrofit.Builder builder = new Retrofit.Builder().baseUrl(BASE_URL);

    private static Interceptor redirectionInterceptor = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);
            if (response.code() == 301) {
                String newUrl = response.header("Location");
                if (!isValidRedirect(request.url(), HttpUrl.parse(newUrl))) {
                    Logger.log(LogTypes.TYPE_WARNING_NETWORK, "Invalid redirect from " + request.url().toString() + " to " + response.request().url().toString());
                    throw new IOException("Invalid redirect from secure server to insecure server");
                }
            }
            return response;
        }
    };

    private static AuthenticationInterceptor authenticationInterceptor = new AuthenticationInterceptor();

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
            .connectTimeout(ModernHttpRequester.CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(ModernHttpRequester.CONNECTION_SO_TIMEOUT, TimeUnit.MILLISECONDS)
            .addNetworkInterceptor(redirectionInterceptor)
            .addInterceptor(authenticationInterceptor)
            .followRedirects(true);

    private static Retrofit retrofit = builder.client(httpClient.build()).build();

    public static CommCareNetworkService createCommCareNetworkService(final String credential) {
        authenticationInterceptor.setCredential(credential);
        return retrofit.create(CommCareNetworkService.class);
    }

    private static boolean isValidRedirect(HttpUrl url, HttpUrl newUrl) {
        //unless it's https, don't worry about it
        if (!url.scheme().equals("https")) {
            return true;
        }

        // If https, verify that we're on the same server.
        // Not being so means we got redirected from a secure link to a
        // different link, which isn't acceptable for now.
        return url.host().equals(newUrl.host());
    }
}
