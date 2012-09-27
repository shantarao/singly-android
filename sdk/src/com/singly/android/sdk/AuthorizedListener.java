package com.singly.android.sdk;

/**
 * A callback interface for the different authorization states that occur when
 * using Singly to authorize with one of the api services, such as facebook.  
 * 
 * When calling the {@link SinglyClient#authorize(String, AuthorizedListener)} 
 * method the AuthorizedLister will be called back during various states.
 */
public interface AuthorizedListener {

  /**
   * Different error types that can occur during the authentication process.
   */
  public static enum Errors {
    NO_NETWORK_PERMISSIONS,
    NO_INTERNET_ACCESS,
    AUTHORIZE_SERVICE_URL,
    NO_ACCESS_TOKEN,
    AUTHENTICATION_ERROR;
  }

  /**
   * Called when the authorization process starts.
   */
  public void onStart();

  /**
   * Called as the authorization page dialog is loading.  This is before the
   * service authentication page has been fully loaded.
   * 
   * @param A number between 1-100 indicating page load progess.  Typically 
   * this is used if you want to show a progress dialog while waiting for the 
   * service authentication page to fully load.
   */
  public void onProgress(int progress);

  /**
   * Called when the service authentication page has fully loaded and the 
   * Authorization dialog is about to be displayed.
   */
  public void onPageLoaded();

  /**
   * Called after the user has authenticated with the service.  This is a 
   * terminal state in the authorization process.
   */
  public void onAuthorized();

  /**
   * Called if there is an error with authorization or if the user was not 
   * able to successfully authenticate with the service.  This is a 
   * terminal state in the authorization process.
   * 
   * @param error The type of error that occured.
   */
  public void onError(Errors error);

  /**
   * Called if the authentication dialog was cancelled by the user.
   */
  public void onCancel();

}
