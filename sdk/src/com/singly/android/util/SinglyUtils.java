package com.singly.android.util;

import java.net.URISyntaxException;
import java.util.Map;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Utility methods for the Singly SDK.
 */
public class SinglyUtils {

  private static final String SINGLY_SCHEME = "https";
  private static final String SINGLY_HOST = "api.singly.com";

  public static String getSinglyScheme() {
    return SINGLY_SCHEME;
  }

  public static String getSinglyHost() {
    return SINGLY_HOST;
  }

  /**
   * Creates a url using the base singly api url and the path.
   * 
   * The url is assumed to be in UTF-8 format.  The query parameters are
   * not required.
   * 
   * @param path The url path.
   * 
   * @return A formatted, UTF-8 singly url string.
   */
  public static String createSinglyURL(String path) {

    // create the formatted UTF-8 url
    try {
      return URLUtils.createURL(SINGLY_SCHEME, SINGLY_HOST, path, null);
    }
    catch (URISyntaxException e) {
      return null;
    }
  }

  /**
   * Creates a url using the base singly api url, the path, and the query
   * parameters specified.
   * 
   * The url is assumed to be in UTF-8 format.  The query parameters are
   * not required.
   * 
   * @param path The url path.
   * @param qparams The optional url query parameters.
   * 
   * @return A formatted, UTF-8 singly url string.
   */
  public static String createSinglyURL(String path,
    Map<String, String> parameters) {

    // create the formatted UTF-8 url
    try {
      return URLUtils.createURL(SINGLY_SCHEME, SINGLY_HOST, path, parameters);
    }
    catch (URISyntaxException e) {
      return null;
    }
  }

  /**
   * Returns true if the context has permission to access the network state and
   * access the internet.
   * 
   * @return True if the context has the correct permissions.
   */
  public static boolean hasNetworkPermissions(Context context) {

    // get permissions for accessing the internet and network state
    int internetPerm = context
      .checkCallingOrSelfPermission(Manifest.permission.INTERNET);
    int networkStatePerm = context
      .checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE);

    // are those permissions allowed
    boolean internetAllowed = internetPerm == PackageManager.PERMISSION_GRANTED;
    boolean networkAllowed = networkStatePerm == PackageManager.PERMISSION_GRANTED;

    // if they are check if we are connected to the network
    return (internetAllowed && networkAllowed);
  }

  /**
   * Returns true if the app is connected to the internet, meaning the network
   * is connected.
   * 
   * @return True if the context can connect to the internet.
   */
  public static boolean isConnectedToInternet(Context context) {

    // check if we are connected to the network
    ConnectivityManager cm = (ConnectivityManager)context
      .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = cm.getActiveNetworkInfo();
    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
      return true;
    }

    // not connected to the network
    return false;
  }

  /**
   * Saves the Singly access token to shared preferences.
   * 
   * @param context The current Android context.
   * @param accessToken The Singly access token.
   */
  public static void saveAccessToken(Context context, String accessToken) {
    if (accessToken != null && accessToken.trim().length() > 0) {
      SharedPreferences prefs = context.getSharedPreferences("singly",
        Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = prefs.edit();
      editor.putString("accessToken", accessToken);
      editor.commit();
    }
  }

  /**
   * Gets the access token from Shared preferences if one exists.
   * 
   * @param context The current Android context.
   * 
   * @return The access token or null if one does not exist.
   */
  public static String getAccessToken(Context context) {
    SharedPreferences prefs = context.getSharedPreferences("singly",
      Context.MODE_PRIVATE);
    return prefs.getString("accessToken", null);
  }
  
  /**
   * Removes the access token from shared preferences.
   * 
   * This is useful in cases where we have an access token but for some reason
   * it isn't valid.
   * 
   * @param context The current Android context.
   */
  public static void clearAccessToken(Context context) {

    SharedPreferences prefs = context.getSharedPreferences("singly",
      Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.remove("accessToken");
    editor.commit();
  }
}
