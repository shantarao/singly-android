package com.singly.android.client;

public interface AsyncApiResponseHandler {

  public void onSuccess(String response);
  
  public void onFailure(Throwable error);
  
}
