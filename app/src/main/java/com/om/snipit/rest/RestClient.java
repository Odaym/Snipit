package com.om.snipit.rest;

import com.om.snipit.classes.Constants;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestClient {
  private SnipitApi_Service apiService;

  public RestClient() {

    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    httpClient.addInterceptor(loggingInterceptor);

    Retrofit retrofit = new Retrofit.Builder().baseUrl(Constants.PRIV_SERVER_BASE_URL)
        .client(httpClient.build())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();

    apiService = retrofit.create(SnipitApi_Service.class);
  }

  public SnipitApi_Service getApiService() {
    return apiService;
  }
}
