package com.singly.android_example;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.singly.sdk.AuthorizedListener;
import com.singly.sdk.SinglyClient;

public class MainActivity
  extends Activity {

  String accessToken;
  Activity activity = this;
  private ProgressDialog progressDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    final SinglyClient api = new SinglyClient(activity,
      "your_client_id", "your_client_secret");

    final Intent profilesIntent = new Intent(MainActivity.this,
      ProfilesActivity.class);

    final Button facebookButton = (Button)findViewById(R.id.facebook);
    facebookButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        api.authorize("facebook", new AuthorizedListener() {

          public void onStartAuthDialog() {
            progressDialog = ProgressDialog.show(activity, "", "Loading...");
          }

          public void onFinishAuthDialog() {
            progressDialog.dismiss();
          }

          public void onAuthorized() {
            MainActivity.this.startActivity(profilesIntent);
          }

          public void onError(String message) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
          }

        });
      }
    });

    final Button githubButton = (Button)findViewById(R.id.github);
    githubButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        api.authorize("github", new AuthorizedListener() {
          
          public void onStartAuthDialog() {
            progressDialog = ProgressDialog.show(activity, "", "Loading...");
          }

          public void onFinishAuthDialog() {
            progressDialog.dismiss();
          }
          
          public void onAuthorized() {
            MainActivity.this.startActivity(profilesIntent);
          }

          public void onError(String message) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
          }
        });
      }
    });

    final Button foursquareButton = (Button)findViewById(R.id.foursquare);
    foursquareButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        api.authorize("foursquare", new AuthorizedListener() {
          
          public void onStartAuthDialog() {
            progressDialog = ProgressDialog.show(activity, "", "Loading...");
          }

          public void onFinishAuthDialog() {
            progressDialog.dismiss();
          }
          
          public void onAuthorized() {
            MainActivity.this.startActivity(profilesIntent);
          }

          public void onError(String message) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
          }
        });
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }

}
