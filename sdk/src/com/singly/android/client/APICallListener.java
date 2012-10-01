package com.singly.android.client;

/**
 * Callback interface for making API calls to the Singly api.
 */
public interface APICallListener {

  /**
   * Called when the API call has successfully returned.
   * 
   * @param response The String response returned from the API call.
   */
  public void onSuccess(String response);

  /**
   * Called when the API call errors and does not return successfully.
   * 
   * @param message The error message.
   */
  public void onError(String message);

}
