package com.singly.android.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.singly.android.util.JSON;
import com.singly.android.util.SinglyHttpClient;

/**
 * A client that handles all authentication with various services through Singly
 * and calls made to the Singly API.
 * 
 * @see https://singly.com/docs/api
 */
public class SinglyClient {

  public static final String AUTH_REDIRECT = "singly://success";

  private static final String BASE_URL = "api.singly.com";
  private static final String TAG = SinglyClient.class.getSimpleName();

  private String clientId;
  private String clientSecret;
  private SharedPreferences prefs;
  private Context context;
  private SinglyHttpClient httpClient;

  /**
   * Returns true if the context has permission to access the network state and
   * access the internet.
   * 
   * @return True if the context has the correct permissions.
   */
  private boolean hasNetworkPermissions() {

    // get permissions for accessing the internet and network state
    int internetPerm = context
      .checkCallingOrSelfPermission(Manifest.permission.INTERNET);
    int networkStatePerm = context
      .checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE);

    // are those permissions allowed
    boolean internetAllowed = internetPerm == PackageManager.PERMISSION_GRANTED;
    boolean networkAllowed = networkStatePerm == PackageManager.PERMISSION_GRANTED;

    // if they are check if we are connected to the network
    return (internetAllowed && networkAllowed);
  }

  /**
   * Returns true if the app is connected to the internet, meaning the network
   * is connected.
   * 
   * @return True if the context can connect to the internet.
   */
  private boolean isConnectedToInternet() {

    // check if we are connected to the network
    ConnectivityManager cm = (ConnectivityManager)context
      .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = cm.getActiveNetworkInfo();
    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
      return true;
    }

    // not connected to the network
    return false;
  }

  /**
   * Creates a url using the base singly api url, the path, and the query
   * parameters specified.
   * 
   * The url is assumed to be in UTF-8 format.  The query parameters are
   * not required.
   * 
   * @param path The url path.
   * @param qparams The optional url query parameters.
   * 
   * @return A formatted, UTF-8 singly url string.
   */
  private String createURL(String path, List<NameValuePair> qparams) {

    String formattedUrl = null;
    try {

      // query parameters are optional
      String query = null;
      if (qparams != null && qparams.size() > 0) {
        query = URLEncodedUtils.format(qparams, "UTF-8");
      }

      // create the formatted UTF-8 url, always https and using base singly
      URI uri = URIUtils.createURI("https", BASE_URL, -1, path, query, null);
      formattedUrl = uri.toASCIIString();
    }
    catch (URISyntaxException e) {
      Log.e(TAG, "Error creating url: " + path, e);
    }

    return formattedUrl;
  }

  /**
   * Creates a new SinglyClient instance using the context, Singly client id, 
   * and Singly client secret passed.
   * 
   * @param context The current Android context.
   * @param clientId The Singly client id.
   * @param clientSecret The Singly client secret.
   */
  public SinglyClient(Context context, String clientId, String clientSecret) {

    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.context = context;
    this.prefs = context.getSharedPreferences("singly", Context.MODE_PRIVATE);
    this.httpClient = new SinglyHttpClient();
    httpClient.initialize();
  }

  /**
   * Called by Android activities to shutdown the Singly client and release
   * resources held.  Usually called during onStop lifecycle event.
   */
  public void shutdown() {
    httpClient.shutdown();
    httpClient = null;
  }

  /**
   * Called to authenticate a user through singly for a specific service.
   * 
   * Authenticate will open a WebView to a service to authenticate a user.  Once
   * the user authenticates with the service, calls to get an oauth access 
   * token are performed in an AsyncTask.  You should be able to perform any
   * main thread activities, such as opening dialogs, in the listener callback
   * methods.
   * 
   * @param service The service to authenticate the user against.
   * @param callback The listener class used to callback at different point
   * during the authentication process.
   */
  public void authenticate(final String service,
    final AuthenticationListener callback) {

    // start of the authentication process
    callback.onStart();

    // fail early if no permissions
    if (!hasNetworkPermissions()) {
      callback.onError(AuthenticationListener.Errors.NO_NETWORK_PERMISSIONS);
      return;
    }

    // fail early if no internet access
    if (!isConnectedToInternet()) {
      callback.onError(AuthenticationListener.Errors.NO_INTERNET_ACCESS);
      return;
    }

    // create the query parameters
    List<NameValuePair> qparams = new ArrayList<NameValuePair>();
    qparams.add(new BasicNameValuePair("client_id", clientId));
    qparams.add(new BasicNameValuePair("redirect_uri", AUTH_REDIRECT));
    qparams.add(new BasicNameValuePair("service", service));

    // create the authentication url
    String authUrl = createURL("/oauth/authorize", qparams);
    if (authUrl == null) {
      callback.onError(AuthenticationListener.Errors.AUTHENTICATE_SERVICE_URL);
      return;
    }

    Log.d(TAG, authUrl);
    new AuthenticationDialog(context, authUrl,
      new AuthenticationDialog.DialogListener() {

        @Override
        public void onProgress(int progress) {
          callback.onProgress(progress);
        }

        @Override
        public void onPageLoaded() {
          callback.onPageLoaded();
        }

        public void onAuthenticated(final String successUrl) {

          AsyncTask authTask = new AsyncTask<Object, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Object... params) {

              // get the auth code from the auth dialog return query parameter
              Uri uri = Uri.parse(successUrl);
              String authCode = uri.getQueryParameter("code");

              // create the access token url
              List<NameValuePair> qparams = new ArrayList<NameValuePair>();
              qparams.add(new BasicNameValuePair("client_id", clientId));
              qparams
                .add(new BasicNameValuePair("client_secret", clientSecret));
              qparams.add(new BasicNameValuePair("code", authCode));

              // access token url, parameters are added to the post instead of
              // url
              String accessTokenUrl = createURL("/oauth/access_token", null);

              String accessToken = null;
              try {

                // do the post to get the access token
                byte[] responseBytes = httpClient.post(accessTokenUrl, qparams);
                JSONObject root = JSON.parse(new String(responseBytes));
                accessToken = JSON.getString(root, "access_token", null);

                // save the access token in the shared preferences
                if (accessToken != null) {
                  SharedPreferences.Editor editor = prefs.edit();
                  editor.putString("accessToken", accessToken);
                  editor.commit();
                }
                else {

                  // no access token, authorization failed
                  return false;
                }

                // authorization succeeded
                return true;
              }
              catch (Exception e) {

                // exception, authorization failed
                return false;
              }
            }

            @Override
            protected void onPostExecute(Boolean result) {

              if (result) {
                callback.onAuthenticated();
              }
              else {
                callback.onError(AuthenticationListener.Errors.NO_ACCESS_TOKEN);
              }
            }
          };

          // execute the auth task
          authTask.execute();
        }

        @Override
        public void onError() {
          callback.onError(AuthenticationListener.Errors.AUTHENTICATION_ERROR);
        }

        @Override
        public void onCancel() {
          callback.onCancel();
        }

      }).show();
  }

  /**
   * Removes any saved authentication state from the Singly client.  After
   * calling clear, to call the api, users will need to re-authenticate with at
   * least one service.
   * 
   * This is useful in cases where we have an access token but for some reason
   * it isn't valid.  This method allows us to clear and then use authenticate
   * to get a new token.
   */
  public void clear() {

    // removes the saved authentication access tokens and keys
    SharedPreferences.Editor editor = prefs.edit();
    editor.remove("accessToken");
    editor.commit();
  }

  /**
   * Performs a Singly api call.
   * 
   * The {@link #authenticate(String, AuthenticationListener)} method must be
   * called at least once for a service before any api calls are made.  Once 
   * authenticated the application stores an access token used to call the 
   * Singly api.  That access token is then appended to any api calls made
   * through this method.
   * 
   * Both GET and POST requests can be performed by specifying the method.  A
   * raw body content, presumably JSON, can also be included.  Any raw body
   * content be ignored if the method is not specified as POST.  If both raw 
   * body content and input parameters are specified, the parameters will be
   * appended to the api call url.  If a method is not specified, GET is used.
   * 
   * This method performs the api call in an asynchronous method and returns
   * any response as a JSONObject to the APICallListener callback.  To perform
   * the call an AsyncTask is used with the success/error callbacks happening
   * in the PostExecute method.  This means clients should be fine performing
   * UI methods, such as Toasts and Dialogs, within the APICallListener callback
   * methods.
   * 
   * @param endpoint The singly api call to make.
   * @param method The HTTP method, either GET or POST.
   * @param rawBody A raw body content to be posted to the API.  The method
   * must be set to POST or any raw body content is ignored.
   * @param parameters The api call parameters.  You do not need to include
   * the access token, that will be automatically appended.
   * @param callback The listener class used to callback on success or error
   * of the api call.
   * 
   * @see https://singly.com/docs/api For documentation on Singly api calls.
   */
  public void apiCall(final String endpoint, final String method,
    final Map<String, String> parameters, final String rawBody,
    final APICallListener callback) {

    // fail early if no permissions
    if (!hasNetworkPermissions()) {
      callback.onError("This application doesn't have permissions to access"
        + " the internet or network state.");
      return;
    }

    // fail early if no internet access
    if (!isConnectedToInternet()) {
      callback.onError("The application cannot connect to the internet.");
      return;
    }

    // fail early for no access token
    String accessToken = prefs.getString("accessToken", null);
    if (accessToken == null) {
      callback.onError("The application must be authenticated with at least one"
        + " service to make api calls.");
      return;
    }

    // are we doing a GET, a POST, or a POST with raw body content
    final boolean isGet = method == null || method.equalsIgnoreCase("GET");
    final boolean isPost = method != null && method.equalsIgnoreCase("POST");
    final boolean isRawPost = isPost && rawBody != null;

    // add the access token to the parameters for every api call
    List<NameValuePair> qparams = new ArrayList<NameValuePair>();
    qparams.add(new BasicNameValuePair("access_token", accessToken));

    // convert input params to name value pairs
    List<NameValuePair> iparams = new ArrayList<NameValuePair>();
    if (parameters != null) {
      for (Map.Entry<String, String> entry : parameters.entrySet()) {
        iparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
      }
    }

    // get methods and post methods that are posting a raw body get all
    // parameters in the url
    if ((isGet || isRawPost) && iparams.size() > 0) {
      qparams.addAll(iparams);
    }

    // create the endpoint url and post params if method is post
    final String apiCallUrl = createURL(endpoint, qparams);
    final List<NameValuePair> postParams = isPost ? iparams : null;

    AsyncTask apiCallTask = new AsyncTask<Object, Integer, String>() {

      @Override
      protected String doInBackground(Object... params) {

        try {

          // perform a call to the api either as a get, post, or post with
          // body content
          Log.d(TAG, "API call to: " + apiCallUrl);
          byte[] responseBytes = null;
          if (isGet) {
            responseBytes = httpClient.get(apiCallUrl);
          }
          else if (isPost && rawBody == null) {
            responseBytes = httpClient.post(apiCallUrl, postParams);
          }
          else if (isPost && rawBody != null) {
            StringEntity jsonBody = new StringEntity(rawBody);
            responseBytes = httpClient.postAsBody(apiCallUrl, jsonBody);
          }

          // no response bytes is an error
          if (responseBytes == null) {
            Log.e(TAG, "Api call returned 0 bytes:" + endpoint);
            return null;
          }

          // pare and return the JSON response
          String response = new String(responseBytes);
          Log.d(TAG, response);

          return response;
        }
        catch (Exception e) {

          // error during the api call, return null which singles error
          String message = e.getMessage();
          Log.e(TAG, "Api call error:" + endpoint + " " + message);
          return null;
        }

      }

      @Override
      protected void onPostExecute(String response) {

        if (response != null) {
          callback.onSuccess(response);
        }
        else {
          callback.onError("Api call error");
        }
      }
    };

    // execute the api call in an async task
    apiCallTask.execute();
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

}
