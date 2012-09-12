package com.singly.android_example;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import com.singly.sdk.*;
import com.singly.sdk.Singly.DialogListener;

public class MainActivity extends Activity {
	
	String accessToken;
	Activity activity = this;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    	final Singly api = new Singly(activity, "YOUR CLIENT_ID","YOUR CLIENT_SECRET");
    	
    	final Intent profilesIntent = new Intent(MainActivity.this, ProfilesActivity.class);


        final Button facebookButton = (Button) findViewById(R.id.facebook);
        facebookButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
            	api.authorize(activity, "facebook", new DialogListener() {
            		public void onComplete(String token) {
             		    MainActivity.this.startActivity(profilesIntent);
            		}
            	});
            }
        });
        
        final Button githubButton = (Button) findViewById(R.id.github);
        githubButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
            	api.authorize(activity, "github", new DialogListener() {
            		public void onComplete(String token) {
             		    MainActivity.this.startActivity(profilesIntent);
            		}
            	});
            }
        });
        
        final Button foursquareButton = (Button) findViewById(R.id.foursquare);
        foursquareButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
            	api.authorize(activity, "foursquare", new DialogListener() {
            		public void onComplete(String token) {
             		    MainActivity.this.startActivity(profilesIntent);
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
