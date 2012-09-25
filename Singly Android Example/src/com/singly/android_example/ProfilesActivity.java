package com.singly.android_example;

import java.util.Iterator;

import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import com.singly.sdk.APICallListener;
import com.singly.sdk.SinglyClient;

public class ProfilesActivity
  extends Activity {

  Activity activity = this;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_profiles);
    final SinglyClient api = new SinglyClient(activity,
      "your_client_id", "your_client_secret");

    api.apiCall("/profiles", null, new APICallListener() {

      public void onSuccess(JSONObject jsonObj) {
        TextView t = (TextView)findViewById(R.id.profiles);
        for (Iterator iter = jsonObj.keys(); iter.hasNext();) {
          String profile = (String)iter.next();
          if (!profile.equals("id")) {
            t.append("\n" + profile);
          }
        }
      }

      public void onError(String message) {

      }
    });

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_profiles, menu);
    return true;
  }

}
