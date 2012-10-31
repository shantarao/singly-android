package com.singly.android.examples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.singly.android.component.AuthenticatedServicesActivity;
import com.singly.android.component.FriendsListActivity;

public class MainActivity
  extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    final Context context = this;

    // example showing all authentication services
    Button fullButton = (Button)findViewById(R.id.fullAuthButton);
    fullButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        Intent authServicesActivity = new Intent(context,
          AuthenticatedServicesActivity.class);
        context.startActivity(authServicesActivity);
      }
    });

    // example showing only specified authentication services
    Button limitedButton = (Button)findViewById(R.id.limitedAuthButton);
    limitedButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        // example of how to pass in scope
        Bundle scopes = new Bundle();
        scopes.putCharSequence("linkedin",
          "r_basicprofile r_fullprofile r_emailaddress");

        Intent authServicesActivity = new Intent(context,
          AuthenticatedServicesActivity.class);
        String[] includedServices = {
          "twitter", "facebook", "github", "linkedin"
        };
        authServicesActivity.putExtra("includes", includedServices);
        authServicesActivity.putExtra("scopes", scopes);
        context.startActivity(authServicesActivity);
      }
    });

    // example showing only specified authentication services
    Button friendsButton = (Button)findViewById(R.id.friendsButton);
    friendsButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        Intent friendsListActivity = new Intent(context,
          FriendsListActivity.class);
        context.startActivity(friendsListActivity);
      }
    });

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }
}
