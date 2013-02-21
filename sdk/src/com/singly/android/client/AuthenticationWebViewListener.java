package com.singly.android.client;

/**
 * A listener interface for when the AuthenticationActivity has completed the
 * authentication process or errored during the completion of authentication.
 */
public interface AuthenticationWebViewListener {

  /**
   * Called when the authentication process has completed.  Allow any cleanup
   * work to be processed, such as closing the AuthenticationActivity.
   */
  public void onFinish();

  /**
   * Called when an error occurs in the completion of the authentication
   * process.
   * 
   * @param error The error that occurred.
   * @param message The string error message.
   */
  public void onError(Throwable error, String message);

}
