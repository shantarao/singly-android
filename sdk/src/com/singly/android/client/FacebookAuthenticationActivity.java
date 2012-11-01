package com.singly.android.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.singly.android.client.SinglyClient.Authentication;
import com.singly.android.util.JSON;

/**
 * Activity that handles authentication to Facebook using the Facebook Android
 * SDK native authentication process.
 * 
 * Values can be passed into this Activity by the Intent that started it.
 * 
 * <ol>
 *   <li>params - An optional map of extra authentication parameters in a
 *   Bundle.  This include parameters such as permissions, which is a string
 *   array of requested Facebook permissions.</li>
 * </ol>    
 * 
 * The FacebookAuthenticationActivity performs the authentication process using
 * native Facebook authentication.  The authentication dialog is opened.  If the
 * user has already logged into their Facebook app they will not need to enter
 * their login credentials.  Once authenticated, the Facebook access token will
 * be applyed to Singly getting a Singly access token and account.  The singly 
 * account id and access token are then stored in the shared preferences of the 
 * app where it can be retrieved to make calls to the Singly API.
 * 
 * Once the access token is saved upon successful authentication or in the case 
 * of Facebook authentication erroring, the FacebookAuthenticationActivity will
 * finish and close itself.
 *
 * To use the FacebookAuthenticationActivity you must register an app with 
 * Facebook and go through their normal signing process.  Your Facebook app id
 * and app secret need to be added to your Singly account.  In the Native
 * Android App in Facebook's setting you would this class's fully qualified
 * class name, "com.singly.android.client.FacebookAuthenticationActivity". The
 * FacebookAuthenticationActivity must also be registered in the manifest of
 * the application using the Singly SDK.
 */
public class FacebookAuthenticationActivity
  extends Activity {

  // android OK result code is -1, so we use -50 and less
  public static final int RESULT_FACEBOOK_ERROR = -50;
  public static final int RESULT_SINGLY_ERROR = -51;
  public static final int RESULT_NO_APPID = -52;

  private Context context;
  private SinglyClient singlyClient;
  private Facebook facebook;
  private Bundle params;

  private class FacebookAuthenticationListener
    implements DialogListener {

    @Override
    public void onComplete(Bundle values) {

      // get the client id, secret, and facebook token
      String fbToken = facebook.getAccessToken();
      Authentication auth = singlyClient.getAuthentication(context);
      Map<String, String> qparams = new HashMap<String, String>();
      qparams.put("client_id", singlyClient.getClientId());
      qparams.put("client_secret", singlyClient.getClientSecret());
      if (StringUtils.isNotBlank(auth.account)) {
        qparams.put("account", auth.account);
      }
      qparams.put("token", fbToken);

      // call the apply method
      singlyClient.doGetApiRequest(context, "/auth/facebook/apply", qparams,
        new AsyncApiResponseHandler() {

          @Override
          public void onSuccess(String response) {

            // get the account and access token from JSON
            JsonNode root = JSON.parse(response);
            String accessToken = JSON.getString(root, "access_token");
            String account = JSON.getString(root, "account");

            // get the shared preferences
            SharedPreferences prefs = context.getSharedPreferences("singly",
              Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // save the account if there is one
            if (StringUtils.isNotBlank(account)) {
              editor.putString(SinglyClient.ACCOUNT, account);
            }

            // save the access token if there is one
            if (StringUtils.isNotBlank(accessToken)) {
              editor.putString(SinglyClient.ACCESS_TOKEN, accessToken);
            }

            // commit changes to shared preferences
            editor.commit();

            // finish and dismiss the facebook auth activity
            FacebookAuthenticationActivity.this.finish();
          }

          @Override
          public void onFailure(Throwable error) {

            // finish and dismiss the facebook auth activity
            FacebookAuthenticationActivity.this.setResult(RESULT_SINGLY_ERROR);
            FacebookAuthenticationActivity.this.finish();
          }
        });

    }

    @Override
    public void onFacebookError(FacebookError e) {

      // finish and dismiss the facebook auth activity
      FacebookAuthenticationActivity.this.setResult(RESULT_FACEBOOK_ERROR);
      FacebookAuthenticationActivity.this.finish();
    }

    @Override
    public void onError(DialogError e) {

      // finish and dismiss the facebook auth activity
      FacebookAuthenticationActivity.this.setResult(RESULT_FACEBOOK_ERROR);
      FacebookAuthenticationActivity.this.finish();
    }

    @Override
    public void onCancel() {

      // finish and dismiss the facebook auth activity if canceled
      FacebookAuthenticationActivity.this.setResult(RESULT_CANCELED);
      FacebookAuthenticationActivity.this.finish();
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    this.context = (Context)this;
    this.singlyClient = SinglyClient.getInstance();
    Intent intent = getIntent();
    this.params = intent.getBundleExtra("params");

    String[] perms = new String[] {};
    if (params != null) {
      perms = params.getStringArray("permissions");
    }

    // get the facebook app id, then authenticate
    String clientId = singlyClient.getClientId();
    String appIdEndpoint = "/auth/" + clientId + "/client_id/facebook";
    final String[] facebookPerms = perms;
    singlyClient.doGetApiRequest(context, appIdEndpoint, null,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {

          // if we couldn't find a facebook app id then finish
          JsonNode root = JSON.parse(response);
          String facebookAppId = JSON.getString(root, "facebook");
          if (StringUtils.isBlank(facebookAppId)) {
            FacebookAuthenticationActivity.this.setResult(RESULT_NO_APPID);
            FacebookAuthenticationActivity.this.finish();
          }

          // if we did find an appid then authorize through the facebook sdk
          facebook = new Facebook(facebookAppId);
          facebook.authorize(FacebookAuthenticationActivity.this,
            facebookPerms, new FacebookAuthenticationListener());
        }

        @Override
        public void onFailure(Throwable error) {

          // finish and dismiss the facebook auth activity
          FacebookAuthenticationActivity.this.setResult(RESULT_NO_APPID);
          FacebookAuthenticationActivity.this.finish();
        }
      });

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    // when the facebook activity closes, perform the callback for error or
    // completion of the auth
    super.onActivityResult(requestCode, resultCode, data);
    facebook.authorizeCallback(requestCode, resultCode, data);
  }
}
