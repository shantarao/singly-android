package com.singly.android.examples;

import java.util.Iterator;

import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.TextView;

import com.singly.android.client.AsyncApiResponseHandler;
import com.singly.android.client.SinglyClient;
import com.singly.android.util.JSON;
import com.singly.android.util.SinglyUtils;

public class ProfilesActivity
  extends Activity {

  private SinglyClient singlyClient;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_profiles);
    singlyClient = new SinglyClient("your_client_id", "your_client_secret");

    singlyClient.doGetApiRequest(this, "/profiles", null,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {
          JSONObject jsonObj = JSON.parse(response);
          TextView t = (TextView)findViewById(R.id.profiles);
          for (Iterator iter = jsonObj.keys(); iter.hasNext();) {
            String profile = (String)iter.next();
            if (!profile.equals("id")) {
              t.append("\n" + profile);
            }
          }
        }

        @Override
        public void onFailure(Throwable error) {

        }
      });

    Button clearButton = (Button)findViewById(R.id.clearAuth);
    clearButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        SinglyUtils.clearAccessToken(ProfilesActivity.this);
        CookieSyncManager.createInstance(ProfilesActivity.this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        ProfilesActivity.this.finish();
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_profiles, menu);
    return true;
  }

}
