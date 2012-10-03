package com.singly.android.examples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.singly.android.client.SinglyClient;

public class MainActivity
  extends Activity {

  private SinglyClient singlyClient;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    singlyClient = new SinglyClient("your_client_id", "your_client_secret");

    Button facebookButton = (Button)findViewById(R.id.facebook);
    facebookButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        singlyClient.authenticate(MainActivity.this, "facebook");
      }
    });

    Button githubButton = (Button)findViewById(R.id.github);
    githubButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        singlyClient.authenticate(MainActivity.this, "github");
      }
    });

    Button foursquareButton = (Button)findViewById(R.id.foursquare);
    foursquareButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        singlyClient.authenticate(MainActivity.this, "foursquare");
      }
    });

    Button showProfilesButton = (Button)findViewById(R.id.showProfiles);
    showProfilesButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        Intent profilesIntent = new Intent(MainActivity.this,
          ProfilesActivity.class);
        startActivity(profilesIntent);
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.d(MainActivity.class.getSimpleName(), "onStart");
    Button showProfilesButton = (Button)findViewById(R.id.showProfiles);
    showProfilesButton.setVisibility(singlyClient.isAuthenticated(this)
      ? View.VISIBLE : View.INVISIBLE);
  }
}
