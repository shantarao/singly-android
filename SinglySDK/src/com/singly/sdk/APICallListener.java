package com.singly.sdk;

import org.json.JSONObject;

public interface APICallListener {

  public void onSuccess(JSONObject jsonObj);

  public void onError(String message);

}
