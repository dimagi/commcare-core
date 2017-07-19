package org.commcare.core.network;

import org.javarosa.core.io.StreamsUtil;

import java.io.InputStream;

import okhttp3.Headers;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class OkHTTPResponseMock {

    public static Response<ResponseBody> createResponse(Integer responseCode) {
        return createResponse(responseCode,"");
    }

    public static Response<ResponseBody> createResponse(Integer responseCode, Headers headers) {
        return Response.success(null, new okhttp3.Response.Builder() //
                .code(responseCode)
                .message("OK")
                .protocol(Protocol.HTTP_1_1)
                .headers(headers)
                .request(new Request.Builder().url("http://localhost/").build())
                .build());
    }

    public static Response<ResponseBody> createResponse(Integer responseCode, InputStream inputStream) {
        ResponseBody responseBody = new FakeResponseBody(inputStream);
        return createResponse(responseCode, responseBody);
    }

    public static Response<ResponseBody> createResponse(Integer responseCode, String body) {
        ResponseBody responseBody = new FakeResponseBody(StreamsUtil.toInputStream(body));
        return createResponse(responseCode, responseBody);
    }

    public static Response<ResponseBody> createResponse(Integer responseCode, ResponseBody responseBody){
        return createResponse(responseCode,responseBody,null);
    }

    public static Response<ResponseBody> createResponse(Integer responseCode, ResponseBody responseBody, Headers headers) {
        if (responseCode < 400) {
            return Response.success(null, new okhttp3.Response.Builder() //
                    .code(responseCode)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .headers(headers)
                    .request(new Request.Builder().url("http://localhost/").build())
                    .build());
        } else {
            return Response.error(responseCode, responseBody);
        }
    }
}
