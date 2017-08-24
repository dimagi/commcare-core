package org.commcare.core.network;

import java.io.InputStream;

import okhttp3.Headers;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Response Factory for OkHTTP Response
 */
public class OkHTTPResponseMockFactory {

    public static Response<ResponseBody> createResponse(Integer responseCode) {
        return createResponse(responseCode, "");
    }

    public static Response<ResponseBody> createResponse(Integer responseCode, Headers headers) {
        return createResponse(responseCode, null, headers);
    }

    public static Response<ResponseBody> createResponse(Integer responseCode, InputStream inputStream) {
        ResponseBody responseBody = new FakeResponseBody(inputStream);
        return createResponse(responseCode, responseBody);
    }

    private static Response<ResponseBody> createResponse(Integer responseCode, String body) {
        ResponseBody responseBody = ResponseBody.create(null, body);
        return createResponse(responseCode, responseBody);
    }

    private static Response<ResponseBody> createResponse(Integer responseCode, ResponseBody responseBody) {
        return createResponse(responseCode, responseBody, null);
    }

    private static Response<ResponseBody> createResponse(Integer responseCode, ResponseBody responseBody, Headers headers) {

        okhttp3.Response.Builder responseBuilder = new okhttp3.Response.Builder() //
                .code(responseCode)
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build());

        if (responseCode < 400) {
            if (headers != null) {
                responseBuilder.headers(headers);
            }
            responseBuilder.message("OK");
            return Response.success(responseBody, responseBuilder.build());
        } else {
            responseBuilder.message("Response.error()");
            return Response.error(responseBody, responseBuilder.build());
        }
    }
}
