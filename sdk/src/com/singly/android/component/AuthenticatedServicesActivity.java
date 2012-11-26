package com.singly.android.component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.LinearLayout;

import com.singly.android.sdk.R;

/**
 * An activity class that uses the AuthenticatedServicesFragment to display a 
 * list of services a use can authenticate with along with the services they 
 * are already authenticated against.
 * 
 * Intent values can be passed in as intent extra information to change the
 * behavior of the AuthenticatedServicesActivity component.  They are:
 * 
 * <ol>
 *   <li>scopes - A bundle of service name to oauth scope parameter.</li>
 *   <li>flags - A bundle of service name to oauth flag parameter.</li>
 *   <li>includes - An array of the service names to include.  Only those
 *   services specified will be displayed.  If not set all services Singly 
 *   provides authentication for are displayed.</li>
 *   <li>useNative - True|False should we use native authentication if and when
 *   it is available.</li>
 * </ol>   
 * 
 * When using the AuthenticatedServicesActivity the activity must be registered 
 * in the AndroidManifest.xml file as follows.
 * 
 * <pre>
 * <activity android:name="com.singly.android.component.AuthenticatedServicesActivity" />
 * </pre>
 */
public class AuthenticatedServicesActivity
  extends FragmentActivity {

  protected Context context;
  protected LinearLayout singlyAuthServicesLayout;
  protected AuthenticatedServicesFragment authServices;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.singly_auth_services_activity);

    // set the context and any scopes specified
    this.context = this;
    Intent intent = getIntent();

    // the id and layouts for the fragments
    int authServicesId = R.id.singlyAuthServicesLayout;
    singlyAuthServicesLayout = (LinearLayout)findViewById(authServicesId);

    FragmentManager fragmentManager = getSupportFragmentManager();

    // setup the friends list fragment
    Fragment authServicesFrag = fragmentManager
      .findFragmentById(authServicesId);
    if (authServicesFrag != null) {
      authServices = (AuthenticatedServicesFragment)authServicesFrag;
    }
    else if (singlyAuthServicesLayout != null) {

      authServices = new AuthenticatedServicesFragment();

      // service specific scopes
      Bundle oauthScopes = intent.getBundleExtra("scopes");
      if (oauthScopes != null) {
        Map<String, String> scopes = new HashMap<String, String>();
        for (String key : oauthScopes.keySet()) {
          scopes.put(key, oauthScopes.getString(key));
        }
        if (!scopes.isEmpty()) {
          authServices.setScopes(scopes);
        }
      }

      // service specific flags
      Bundle oauthFlags = intent.getBundleExtra("flags");
      if (oauthFlags != null) {
        Map<String, String> flags = new HashMap<String, String>();
        for (String key : oauthFlags.keySet()) {
          flags.put(key, oauthFlags.getString(key));
        }
        if (!flags.isEmpty()) {
          authServices.setFlags(flags);
        }
      }

      // native authentication if available
      boolean useNativeAuth = intent.getBooleanExtra("useNativeAuth", false);
      authServices.setUseNativeAuth(useNativeAuth);

      // setup a service inclusion list
      String[] includesAr = intent.getStringArrayExtra("includes");
      if (includesAr != null && includesAr.length > 0) {
        Set<String> includedSet = new HashSet<String>(Arrays.asList(includesAr));
        authServices.setIncludedServices(includedSet);
      }

      // add the fragment
      FragmentTransaction authServicesTrans = fragmentManager.beginTransaction();
      authServicesTrans.add(authServicesId, authServices);
      authServicesTrans.commit();
    }
  }

}
