package org.commcare.core.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthenticationInterceptor implements Interceptor {

    private String credential;

    public AuthenticationInterceptor(String credential) {
        this.credential = credential;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        // NOTE PLM: we will eventually support 'http' urls, but won't include authentication credentials in them
        if (!original.isHttps() && credential !=null) {
            throw new PlainTextPasswordException();
        }

        Request.Builder builder = original.newBuilder()
                .header("Authorization", credential);

        Request request = builder.build();
        return chain.proceed(request);
    }

    public static class PlainTextPasswordException extends RuntimeException {
    }
}
