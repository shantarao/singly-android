package com.singly.android_example;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.singly.sdk.Singly;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class ProfilesActivity extends Activity {
	
	Activity activity = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);
     	final Singly api = new Singly(activity, "YOUR CLIENT_ID","YOUR CLIENT_SECRET");
     	TextView t = (TextView)findViewById(R.id.profiles);
     	
     	JSONObject jArray = api.get("/profiles", null);
		
		for (Iterator iter = jArray.keys(); iter.hasNext();) {
			String profile = (String) iter.next();
			if (!profile.equals("id")) {
				t.append("\n" + profile);
			}
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_profiles, menu);
        return true;
    }

    
}
