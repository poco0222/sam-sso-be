package com.yr.common.utils.http;

import okhttp3.*;
import java.util.Map;
import java.util.Objects;

public class OkHttpUtlis {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static Object doPostJSON(String url, String json, Map<String, String> headerParam) {
        try {
            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(JSON, json);
            Headers headers = Headers.of(headerParam);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .headers(headers)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 构造POST 表单请求
     * @param url url
     * @param fromParam form 表单参数
     * @param headers 自定义请求头
     * @return 响应流
     */
    public static Response doPostForm(String url, Map<String, Object> fromParam, Map<String, Object> headers) {
        try {
            //构造请求参数
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
            //构造请求体
            MultipartBody.Builder mBuilder = new MultipartBody.Builder();
            mBuilder.setType(MultipartBody.FORM);
            if (fromParam != null && fromParam.size() > 0) {
                fromParam.forEach((k, v) -> mBuilder.addFormDataPart(k, String.valueOf(v)));
            }

            MultipartBody multipartBody = mBuilder.build();

            Request.Builder builder  = new Request.Builder();
            if (headers != null && headers.size() > 0) {
                headers.forEach((k, v) -> builder.addHeader(k, String.valueOf(v)));
            }

            Request request = builder.url(urlBuilder.build().toString()).post(multipartBody).build();

            //发出同步请求
            return new OkHttpClient.Builder().build().newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
