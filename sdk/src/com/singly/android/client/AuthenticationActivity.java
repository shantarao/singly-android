package com.singly.android.client;

import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.singly.android.util.SinglyUtils;

/**
 * Activity that handles Singly authentication to various services by opening
 * WebViews and performing oauth.
 * 
 * Three values must be passed into the activity by the Intent that started it.
 * 
 * <ol>
 *   <li>clientId - Holding the Singly app client id.</li>
 *   <li>clientSecret - Holding the Singly app client secret.</li>
 *   <li>service - The name of the service to authenticate the user
 *   against (i.e. facebook)</li>
 * </ol>    
 * 
 * The AuthenticationActivity opens a WebView to the authentication
 * service.  The user then authenticates through a standard oauth process.
 * Once authenticated the Singly access token is retrieved and stored in the
 * Singly shared preferences for the app.  The access token is then used to 
 * make calls to the Singly API.
 * 
 * A progress dialog is shown as the authentication webpage is loading.
 * 
 * Once the access token is saved upon successful authentication or in the case 
 * of the URL erroring, the AuthenticationActivity is finished.
 * 
 * The full authentication process is three steps.  One, open a WebView to the
 * service for the user to authenticate.  Two, once the user authenticates the
 * WebView is redirected to the success URL with the authentication code as a 
 * query parameter.  Three, the authentication code is parsed from the 
 * success URL and a call is made to retrieve the access token.  The access 
 * token is then stored and used to make calls to the Singly API.
 * 
 * Expert: The AuthenticationActivity handles all authentication steps. If you 
 * need a more fine grained control of the authentication process, the 
 * AuthenticationActivity can be subclassed and you can provide your own 
 * WebClientView and WebChromeView implementations by overriding 
 * {@link #getWebViewClient()} and {@link #getWebChromeClient()} respectively.
 * 
 * The {@link BaseAuthenticationWebViewClient} can be extended to create your
 * own WebViewClient implementation. 
 * 
 * @see {@link SinglyUtils#saveAccessToken(Context, String)}
 * @see {@link SinglyUtils#getAccessToken(Context)}
 */
public class AuthenticationActivity
  extends Activity {

  protected static final String SUCCESS_REDIRECT = "singly://success";

  // components and layouts
  private WebView webView;
  private ProgressDialog progressDialog;

  // Singly authentication variables
  private String clientId;
  private String clientSecret;
  private String service;
  private Context context;

  private class AuthenticationWebViewClient
    extends BaseAuthenticationWebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {

      // on successful authentication we should get the redirect url
      if (url.startsWith(SUCCESS_REDIRECT)) {

        // get the auth code from the auth dialog return query parameter
        Uri uri = Uri.parse(url);
        String authCode = uri.getQueryParameter("code");

        completeAuthentication(context, authCode, clientId, clientSecret,
          new AuthenticationWebViewListener() {

            @Override
            public void onFinish() {
              // done with the authentication process, close this activity
              AuthenticationActivity.this.finish();
            }

            @Override
            public void onError(Throwable error) {

              // show toast for error saving the authentication tokens
              Toast.makeText(AuthenticationActivity.this,
                "Error completing authentication", Toast.LENGTH_LONG).show();

              // page errored close this activity
              AuthenticationActivity.this.finish();
            }
          });

        // we handled the url ourselves, don't load the page in the web view
        return true;
      }

      // any other page, load it into the web view
      return false;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode,
      String description, String failingUrl) {

      // finish the activity on error
      super.onReceivedError(view, errorCode, description, failingUrl);

      // dismiss any progress dialog
      progressDialog.dismiss();

      // show toast for error
      Toast.makeText(AuthenticationActivity.this,
        "Error opening authentication webpage", Toast.LENGTH_LONG).show();

      // page errored close this activity
      AuthenticationActivity.this.finish();
    }

    @Override
    public void onPageFinished(WebView view, String url) {

      // don't show the web view until the authentication page loaded
      super.onPageFinished(view, url);
      progressDialog.dismiss();
      webView.setVisibility(View.VISIBLE);
    }
  }

  private class AuthenticationWebChromeClient
    extends WebChromeClient {
    public void onProgressChanged(WebView view, int progress) {
      progressDialog.setProgress(progress);
    }
  }

  protected WebViewClient getWebViewClient() {
    return new AuthenticationWebViewClient();
  }

  protected WebChromeClient getWebChromeClient() {
    return new AuthenticationWebChromeClient();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    this.context = (Context)this;
    Intent intent = getIntent();
    this.clientId = intent.getStringExtra("clientId");
    this.clientSecret = intent.getStringExtra("clientSecret");
    this.service = intent.getStringExtra("service");

    // if the client id, client secret, or service are not passed then the
    // activity immediately exits
    if (this.clientId == null || this.clientSecret == null
      || this.service == null) {
      this.finish();
    }

    Map<String, String> qparams = new LinkedHashMap<String, String>();
    qparams.put("client_id", clientId);
    qparams.put("redirect_uri", SUCCESS_REDIRECT);
    qparams.put("service", service);

    // create the authentication url
    String authUrl = SinglyUtils.createSinglyURL("/oauth/authorize", qparams);

    // no window title
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    // the main web view container layout
    LinearLayout mainLayout = new LinearLayout(context);
    mainLayout.setPadding(0, 0, 0, 0);

    // frame layout around web view
    FrameLayout.LayoutParams frame = new FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    // progress dialog for loading web page
    progressDialog = new ProgressDialog(this);
    progressDialog.setMessage("Loading Authentication");
    progressDialog.setCancelable(false);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progressDialog.setProgress(0); // set percentage completed to 0%

    // the web view with the oauth web page
    webView = new WebView(context);
    webView.setVisibility(View.INVISIBLE);
    webView.setVerticalScrollBarEnabled(false);
    webView.setHorizontalScrollBarEnabled(false);
    webView.setWebViewClient(getWebViewClient());
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setLayoutParams(frame);
    webView.getSettings().setSavePassword(false);
    webView.loadUrl(authUrl);

    // register a WebChromeClient to show a progress dialog while loading
    webView.setWebChromeClient(getWebChromeClient());

    // add the web view to the main layout
    mainLayout.addView(webView);

    // set the main layout as the content view
    setContentView(mainLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
      LayoutParams.MATCH_PARENT));

    // show the progress dialog
    progressDialog.show();
  }

}
