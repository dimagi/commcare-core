package org.commcare.core.network;

import java.io.IOException;

import javax.annotation.Nullable;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthenticationInterceptor implements Interceptor {

    @Nullable
    private String credential;
    private boolean enforceSecureEndpoint;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        // if credentials are null, proceed without authenticating
        if (credential == null) {
            return chain.proceed(original);
        }

        // Throw an exception if we are sending an authenticated request over HTTP
        if (enforceSecureEndpoint && !original.isHttps() && credential != null) {
            throw new PlainTextPasswordException();
        }

        Request.Builder builder = original.newBuilder()
                .header("Authorization", credential);

        Request request = builder.build();
        return chain.proceed(request);
    }

    public void setEnforceSecureEndpoint(boolean enforceSecureEndpoint) {
        this.enforceSecureEndpoint = enforceSecureEndpoint;
    }

    public void setCredential(@Nullable String credential) {
        this.credential = credential;
    }

    public static class PlainTextPasswordException extends IOException {
    }
}
