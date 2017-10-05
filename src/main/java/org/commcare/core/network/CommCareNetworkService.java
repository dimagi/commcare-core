package org.commcare.core.network;


import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.QueryMap;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface CommCareNetworkService {

    @Streaming
    @GET
    Call<ResponseBody> makeGetRequest(@Url String url, @QueryMap Map<String, String> params,
                                      @HeaderMap Map<String, String> headers);

    @Streaming
    @Multipart
    @POST
    Call<ResponseBody> makeMultipartPostRequest(@Url String url, @QueryMap Map<String, String> params,
                                                @HeaderMap Map<String, String> headers,
                                                @Part List<MultipartBody.Part> files);

    @Streaming
    @POST
    Call<ResponseBody> makePostRequest(@Url String url, @QueryMap Map<String, String> params,
                                       @HeaderMap Map<String, String> headers,
                                       @Body RequestBody body);
}
