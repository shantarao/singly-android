package com.singly.sdk;

import org.json.JSONObject;

/**
 * Callback interface for making API calls to the Singly api.
 */
public interface APICallListener {

  /**
   * Called when the API call has successfully returned.
   * 
   * @param jsonObj The JSONObject returned from the API.
   */
  public void onSuccess(JSONObject jsonObj);

  /**
   * Called when the API call errors and does not return successfully.
   * 
   * @param message The error message.
   */
  public void onError(String message);

}
