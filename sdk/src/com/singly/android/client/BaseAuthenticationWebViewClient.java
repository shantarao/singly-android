package com.singly.android.client;

import org.codehaus.jackson.JsonNode;

import android.content.Context;
import android.webkit.WebViewClient;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.singly.android.util.JSONUtils;
import com.singly.android.util.SinglyUtils;

/**
 * An abstract WebViewClient implementation that handles the final part of the
 * Singly Authentication process.  This involves making a call to singly to get
 * the access token once we have an authentication code.
 */
public abstract class BaseAuthenticationWebViewClient
  extends WebViewClient {

  /**
   * When extending this class, you will need to override the {@link 
   * #shouldOverrideUrlLoading(android.webkit.WebView, String)} method.  This
   * completeAuthentication method will then need to be called when the WebView 
   * is redirected to the success URL to complete the authentication process.
   * 
   * @param context The current android context.
   * @param authCode The authentication code.
   * @param clientId The Singly app client id.
   * @param clientSecret The Singly app client secret.
   * @param listener A callback class that notifies when the authentication 
   * process either completes or errors.
   */
  protected void completeAuthentication(final Context context, String authCode,
    String clientId, String clientSecret,
    final AuthenticationWebViewListener listener) {

    // create the post parameters
    RequestParams qparams = new RequestParams();
    qparams.put("client_id", clientId);
    qparams.put("client_secret", clientSecret);
    qparams.put("code", authCode);

    // create the access token url
    String accessTokenUrl = SinglyUtils.createSinglyURL("/oauth/access_token",
      null);

    // make an async http call to get the access token
    AsyncHttpClient client = new AsyncHttpClient();
    client.post(accessTokenUrl, qparams, new AsyncHttpResponseHandler() {

      @Override
      public void onSuccess(String response) {

        // get the access token and put it into the shared preferences
        if (response != null) {
          JsonNode root = JSONUtils.parse(response);
          String accessToken = JSONUtils.getString(root, "access_token");
          SinglyUtils.saveAccessToken(context, accessToken);
        }

        // done with the authentication process, perform finish callback
        listener.onFinish();
      }

      @Override
      public void onFailure(Throwable error) {

        // error getting the access token
        listener.onError(error);
      }

    });
  }
}
