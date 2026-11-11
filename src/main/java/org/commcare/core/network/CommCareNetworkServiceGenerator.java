package org.commcare.core.network;

import static org.javarosa.core.model.utils.DateUtils.HOUR_IN_MS;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import org.commcare.core.services.CommCarePreferenceManagerFactory;
import org.commcare.core.services.ICommCarePreferenceManager;
import org.commcare.util.LogTypes;
import org.javarosa.core.services.Logger;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;

/**
 * Provides an instance of CommCareNetworkService.
 * We have declared everything static in this class as we want to use the same objects (OkHttpClient, Retrofit, …) throughout the app
 * to just open one socket connection that handles all the request and responses.
 */

public class CommCareNetworkServiceGenerator {

    public static final String CURRENT_DRIFT = "current_drift";
    public static final String MAX_DRIFT_SINCE_LAST_HEARTBEAT = "max_drift_since_last_heartbeat";

    // Retrofit needs a base url to generate an instance but since our apis are fully dynamic it's not getting used.
    private static final String BASE_URL = "http://example.url/";

    private static Multimap<String, String> queryParams = ArrayListMultimap.create();

    private static Retrofit.Builder builder = new Retrofit.Builder().baseUrl(BASE_URL);

    private static Interceptor redirectionInterceptor = chain -> {
        Request request = chain.request();
        Response response = chain.proceed(request);
        if (response.code() == 301) {
            String newUrl = response.header("Location");
            if (!isValidRedirect(request.url(), HttpUrl.parse(newUrl))) {
                Logger.log(LogTypes.TYPE_WARNING_NETWORK, "Invalid redirect from " + request.url().toString() + " to " + newUrl);
                throw new IOException("Invalid redirect from secure server to insecure server");
            }
        }
        return response;
    };

    // Retrofit doesn't support multimaps @Querymap, so add the params on fly instead
    private static Interceptor queryParamsInterceptor = chain -> {
        HttpUrl.Builder builder = chain.request().url().newBuilder();
        for (Map.Entry<String, String> entry : queryParams.entries()) {
            builder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        HttpUrl urlWithQueryParams = builder.build();
        Request request = chain.request().newBuilder().url(urlWithQueryParams).build();
        return chain.proceed(request);
    };

    private static Interceptor driftInterceptor = chain -> {
        Request request = chain.request();
        Response response = chain.proceed(request);
        ICommCarePreferenceManager commCarePreferenceManager = CommCarePreferenceManagerFactory.getCommCarePreferenceManager();
        if (commCarePreferenceManager != null) {
            String serverDate = response.header("date");

            try {
                long serverTimeInMillis = new SimpleDateFormat(
                        "EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).parse(serverDate).getTime();
                long now = new Date().getTime();
                long currentDrift = (now - serverTimeInMillis) / HOUR_IN_MS;
                commCarePreferenceManager.putLong(CURRENT_DRIFT, currentDrift);
                long maxDriftSinceLastHeartbeat = commCarePreferenceManager.getLong(MAX_DRIFT_SINCE_LAST_HEARTBEAT, 0);
                currentDrift *= currentDrift < 0 ? -1 : 1; // make it positive to calculate max drift
                if (currentDrift > maxDriftSinceLastHeartbeat) {
                    commCarePreferenceManager.putLong(MAX_DRIFT_SINCE_LAST_HEARTBEAT, currentDrift);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return response;
    };



    private static AuthenticationInterceptor authenticationInterceptor = new AuthenticationInterceptor();

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
            .connectTimeout(ModernHttpRequester.CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(ModernHttpRequester.CONNECTION_SO_TIMEOUT, TimeUnit.MILLISECONDS)
            .addNetworkInterceptor(redirectionInterceptor)
            .addInterceptor(queryParamsInterceptor)
            .addInterceptor(authenticationInterceptor)
            .addInterceptor(driftInterceptor)
            .followRedirects(true);



    private static Retrofit retrofit = builder.client(
            httpClient.retryOnConnectionFailure(true).build())
            .build();


    private static Retrofit noRetryRetrofit = builder.client(
            httpClient.retryOnConnectionFailure(false).build())
            .build();

    public static void customizeRetrofitSetup(HttpBuilderConfig config) {
        retrofit =  builder.client(config.performCustomConfig(httpClient.retryOnConnectionFailure(true)).build()).build();
        noRetryRetrofit =  builder.client(config.performCustomConfig(httpClient.retryOnConnectionFailure(false)).build()).build();
    }

    public static CommCareNetworkService createCommCareNetworkService(final String credential,
            boolean enforceSecureEndpoint, boolean retry,
            Multimap<String, String> params) {
        queryParams = params;
        authenticationInterceptor.setCredential(credential);
        authenticationInterceptor.setEnforceSecureEndpoint(enforceSecureEndpoint);
        if (retry) {
            return retrofit.create(CommCareNetworkService.class);
        } else {
            return noRetryRetrofit.create(CommCareNetworkService.class);
        }
    }

    public static CommCareNetworkService createNoAuthCommCareNetworkService() {
        return createCommCareNetworkService(null, false, true, ImmutableMultimap.of());
    }

    public static CommCareNetworkService createNoAuthCommCareNetworkService(Multimap<String, String> params) {
        return createCommCareNetworkService(null, false, true, params);
    }

    private static boolean isValidRedirect(HttpUrl url, HttpUrl newUrl) {
        // unless it's https, don't worry about it
        if (!url.scheme().equals("https")) {
            return true;
        }

        // If https, verify that we're on the same server.
        // Not being so means we got redirected from a secure link to a
        // different link, which isn't acceptable for now.
        return url.host().equals(newUrl.host());
    }

}
