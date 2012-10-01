package com.singly.android.examples;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.singly.android.client.AuthenticationListener;
import com.singly.android.client.SinglyClient;

public class MainActivity
  extends Activity {

  String accessToken;
  Activity activity = this;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    final SinglyClient api = new SinglyClient(activity,
      "your_client_id",
      "your_client_secret");

    final Intent profilesIntent = new Intent(MainActivity.this,
      ProfilesActivity.class);

    final Button facebookButton = (Button)findViewById(R.id.facebook);
    facebookButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {

        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("Loading Authentication");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0); // set percentage completed to 0%

        api.authenticate("facebook", new AuthenticationListener() {

          public void onStart() {
            progressDialog.show();
          }

          public void onProgress(int progress) {
            progressDialog.setProgress(progress);
          }

          public void onPageLoaded() {
            progressDialog.dismiss();
          }

          public void onAuthenticated() {
            MainActivity.this.startActivity(profilesIntent);
          }

          public void onError(AuthenticationListener.Errors error) {
            String msg = error.toString();
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
          }

          public void onCancel() {
            progressDialog.dismiss();
            Toast.makeText(activity, "Authentication Cancelled",
              Toast.LENGTH_LONG).show();
          }
        });
      }
    });

    final Button githubButton = (Button)findViewById(R.id.github);
    githubButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {

        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("Loading Authentication");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0); // set percentage completed to 0%

        api.authenticate("github", new AuthenticationListener() {

          public void onStart() {
            progressDialog.show();
          }

          public void onProgress(int progress) {
            progressDialog.setProgress(progress);
          }

          public void onPageLoaded() {
            progressDialog.dismiss();
          }

          public void onAuthenticated() {
            MainActivity.this.startActivity(profilesIntent);
          }

          public void onError(AuthenticationListener.Errors error) {
            String msg = error.toString();
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
          }

          public void onCancel() {
            progressDialog.dismiss();
            Toast.makeText(activity, "Authentication Cancelled",
              Toast.LENGTH_LONG).show();
          }
        });
      }
    });

    final Button foursquareButton = (Button)findViewById(R.id.foursquare);
    foursquareButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {

        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("Loading Authentication");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0); // set percentage completed to 0%

        api.authenticate("foursquare", new AuthenticationListener() {

          public void onStart() {
            progressDialog.show();
          }

          public void onProgress(int progress) {
            progressDialog.setProgress(progress);
          }

          public void onPageLoaded() {
            progressDialog.dismiss();
          }

          public void onAuthenticated() {
            MainActivity.this.startActivity(profilesIntent);
          }

          public void onError(AuthenticationListener.Errors error) {
            String msg = error.toString();
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
          }

          public void onCancel() {
            progressDialog.dismiss();
            Toast.makeText(activity, "Authentication Cancelled",
              Toast.LENGTH_LONG).show();
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
