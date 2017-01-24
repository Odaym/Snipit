package com.om.snipit.rest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DefaultGetResponse {

  @SerializedName("data") @Expose private DefaultGetResponseData data;

  /**
   * @return The data
   */
  public DefaultGetResponseData getData() {
    return data;
  }

  /**
   * @param data The data
   */
  public void setData(DefaultGetResponseData data) {
    this.data = data;
  }
}