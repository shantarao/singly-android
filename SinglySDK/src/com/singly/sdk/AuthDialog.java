package com.singly.sdk;

import android.app.Dialog;
import android.content.Context;
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

/**
 * A Dialog class that performs the authentication to a service, such as 
 * facebook, through a WebView.
 * 
 * Methods are provided for callbacks at various stages of the authentication
 * process through the {@link DialogListener}. This is an internal class that 
 * is used by the Singly client.  Any clients to the Singly API should use the 
 * {@link SinglyClient} class for authentication.
 */
class AuthDialog
  extends Dialog {

  static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

  private String authUrl;
  private DialogListener dialogListener;
  private WebView webView;

  /**
   * Listener class that provides callbacks at various stages of the service
   * authentication process.
   */
  public static interface DialogListener {

    /**
     * Called when the service authentication web page is finished loading.
     */
    public void onPageLoaded();

    /**
     * Called when the user has authenticated with the service.
     * 
     * @param url The callback url containing the code used to get the access
     * token for the service based on the user authentication.
     */
    public void onAuthorized(String url);

    /**
     * Called if an error occurs during the authentication process.
     */
    public void onError();

    /**
     * Called as the service authentication page loading progresses.  Once the
     * page is loaded this should be at 100%.
     * 
     * @param progress A number 1-100 that progresses as the page loads.
     */
    public void onProgress(int progress);

    /**
     * Called if the user cancels the authentication dialog.
     */
    public void onCancel();
  }

  /**
   * A WebViewClient class that receives notifications from various events in
   * the WebView.  The AuthWebViewClient translates these into DialogListener
   * callbacks.
   */
  private class AuthWebViewClient
    extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      
      // on successful authentication we should get the redirect url
      if (url.startsWith(SinglyClient.AUTH_REDIRECT)) {
        dialogListener.onAuthorized(url);
        AuthDialog.this.dismiss();
        return true;
      }
      
      // not successful authentication
      return false;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode,
      String description, String failingUrl) {
      
      // dismiss the dialog on error
      super.onReceivedError(view, errorCode, description, failingUrl);
      dialogListener.onError();
      AuthDialog.this.dismiss();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
      
      // don't show the web view until the authentication page loaded
      super.onPageFinished(view, url);
      dialogListener.onPageLoaded();
      webView.setVisibility(View.VISIBLE);
    }
  }

  public AuthDialog(Context context, String authUrl,
    DialogListener dialogListener) {
    super(context, android.R.style.Theme_Translucent_NoTitleBar);
    this.authUrl = authUrl;
    this.dialogListener = dialogListener;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // no window title
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    // the main web view container layout
    LinearLayout mainLayout = new LinearLayout(getContext());
    mainLayout.setPadding(0, 0, 0, 0);

    // the web view with the oauth web page
    webView = new WebView(getContext());
    webView.setVisibility(View.INVISIBLE);
    webView.setVerticalScrollBarEnabled(false);
    webView.setHorizontalScrollBarEnabled(false);
    webView.setWebViewClient(new AuthWebViewClient());
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setLayoutParams(FILL);
    webView.getSettings().setSavePassword(false);
    webView.loadUrl(authUrl);

    webView.setWebChromeClient(new WebChromeClient() {
      public void onProgressChanged(WebView view, int progress) {
        dialogListener.onProgress(progress);
      }
    });

    // add the web view to the main layout
    mainLayout.addView(webView);

    // set the main layout as the content view
    setContentView(mainLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
      LayoutParams.MATCH_PARENT));
  }

  @Override
  public void cancel() {
    super.cancel();
    dialogListener.onCancel();
  }

}