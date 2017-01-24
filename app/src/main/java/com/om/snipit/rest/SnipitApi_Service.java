package com.om.snipit.rest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SnipitApi_Service {

  @POST("syncBookData") Call<DefaultGetResponse> uploadBook(
      @Body ApiCallsHandler.JSONBookRequestParent json_book);
}
