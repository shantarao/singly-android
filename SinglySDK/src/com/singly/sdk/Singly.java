package com.singly.sdk;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

public class Singly {
	public static final String AUTHENTICATION_URL = "https://api.singly.com/oauth/authorize";
	public static final String BASE_URI = "https://api.singly.com";
	public static final String REDIRECT_URI = "singly://success";
	
	private String CLIENT_ID = "";
	private String CLIENT_SECRET = "";
	
	private String mAccessToken = null;
	private DialogListener mAuthDialogListener;
	
	SharedPreferences settings = null;
	
	public Singly(Activity activity, String CLIENT_ID, String CLIENT_SECRET) {
		this.CLIENT_ID = CLIENT_ID;
		this.CLIENT_SECRET = CLIENT_SECRET;
		settings = activity.getSharedPreferences("SINGLY", 0);
	}
	
	public void authorize(Activity activity, String service, final DialogListener listener) {
		CookieSyncManager.createInstance(activity);
		final SharedPreferences.Editor editor = settings.edit();
		mAuthDialogListener = listener;
        dialog(activity, service, new DialogListener() {
            public void onComplete(String url_string) {
                CookieSyncManager.getInstance().sync();
                Uri uri = Uri.parse(url_string);
                String code = uri.getQueryParameter("code");
                requestAccessToken(code);
                editor.putString("accessToken", mAccessToken);
    		    editor.commit();
                mAuthDialogListener.onComplete(mAccessToken);
            }
        });
	}
	
	

	public void dialog(Context context, String service,
            final DialogListener listener) {
        
        String url = AUTHENTICATION_URL;
        url += "?client_id=" + CLIENT_ID;
        url += "&redirect_uri=" + REDIRECT_URI;
        url += "&service=" + service;
        if (context.checkCallingOrSelfPermission(Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
        	Toast.makeText(context, "Application requires access to the internet", 
        			Toast.LENGTH_SHORT).show();
        } else {
            new AuthDialog(context, url, listener).show();
        }
    }
	
    public String getAccessToken() {
        return mAccessToken;
    }
    
    public void requestAccessToken(String code) {
        String token = null;
		
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	    nameValuePairs.add(new BasicNameValuePair("client_id", CLIENT_ID)); 
	    nameValuePairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET)); 
	    nameValuePairs.add(new BasicNameValuePair("code", code)); 
		
		//initialize

		JSONObject jArray = null;

		String result = request("POST", "/oauth/access_token", null, nameValuePairs);
		//try parse the string to a JSON object
		try{
	        jArray = new JSONObject(result);
	        token = jArray.getString("access_token");
		} catch(JSONException e){
			Log.e("log_tag", "Error parsing data "+e.toString());
		}
		mAccessToken = token;
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("accessToken", token);
	    editor.commit();
		
    }
    
    public String request(String method, String endpoint, String params, ArrayList<NameValuePair> nameValuePairs) {
		String url = BASE_URI + endpoint;
    	
    	InputStream is = null;
		String result = "";
		HttpEntity entity = null;
		
		mAccessToken = settings.getString("accessToken", null);
		
		//http post
		try{
			HttpClient httpclient = new DefaultHttpClient();
			if (method.equals("POST")) {
				HttpPost httppost = new HttpPost(url);
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				if (nameValuePairs != null) {
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				}
				HttpResponse response = httpclient.execute(httppost);
				entity = response.getEntity();
			}
			else {
				url += "?access_token=" + mAccessToken;
				if (params != null) {
					url += "&" + params;
				}
				HttpGet httpget = new HttpGet(url);
				HttpResponse response = httpclient.execute(httpget);
				entity = response.getEntity();
			}
			
			is = entity.getContent();

		}catch(Exception e){
			Log.e("log_tag", "Error in http connection "+e.toString());
		}

		//convert response to string
		try{
			BufferedReader reader = new BufferedReader(new InputStreamReader(is,"utf-8"));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			result=sb.toString();
		}catch(Exception e){
			Log.e("log_tag", "Error converting result "+e.toString());
		}
		
		return result;
    }
    
    public JSONObject get(String endpoint, String params) {
     	JSONObject jArray = null;
		try{
	        jArray = new JSONObject(request("GET", endpoint, params, null));
		} catch(JSONException e){
			Log.e("log_tag", "Error parsing data "+e.toString());
		}
    	return jArray;
    }
    
    public static interface DialogListener {
		public void onComplete(String url);
    }
}