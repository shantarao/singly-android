package com.singly.android.util;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import android.util.Log;

/**
 * An HTTP client class that handles GET and POST calls.
 * 
 * The client is thread safe,  reuses connections to the same server, sends it 
 * content in UTF-8, uses HTTP 1.1 if available, and will retry connections that
 * error up to 3 times by default.  
 * 
 * The default user agent is Singly-Android-SDK.
 */
public class SinglyHttpClient {

  private static final String TAG = SinglyHttpClient.class.getSimpleName();

  private int connectionTimeoutInSeconds = 20;
  private int socketTimeoutInSeconds = 10;
  private boolean tcpNoDelay = true;
  private int soLingerInSeconds = 0;
  private int maxRetries = 3;

  private String userAgent = "Singly-Android-SDK";

  ThreadSafeClientConnManager connMgr;
  private DefaultHttpClient httpClient;

  public SinglyHttpClient() {
    initialize();
  }

  /**
   * Initializes the SinglyHttpClient.  This method must be called before any
   * request methods are called.
   */
  public void initialize() {

    HttpParams params = new BasicHttpParams();

    params.setParameter(HttpProtocolParams.USER_AGENT, userAgent);
    params.setParameter(HttpProtocolParams.PROTOCOL_VERSION,
      HttpVersion.HTTP_1_1);
    params.setParameter(HttpProtocolParams.HTTP_CONTENT_CHARSET, "UTF-8");
    params.setParameter("http.protocol.cookie-policy",
      CookiePolicy.BROWSER_COMPATIBILITY);
    params.setBooleanParameter("http.protocol.single-cookie-header", true);
    params.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT,
      connectionTimeoutInSeconds * 1000);
    params.setIntParameter(HttpConnectionParams.SO_LINGER, soLingerInSeconds);
    params.setBooleanParameter(HttpConnectionParams.TCP_NODELAY, tcpNoDelay);
    params.setIntParameter(HttpConnectionParams.SO_TIMEOUT,
      socketTimeoutInSeconds * 1000);

    SchemeRegistry registry = new SchemeRegistry();
    PlainSocketFactory plain = PlainSocketFactory.getSocketFactory();
    SSLSocketFactory ssl = SSLSocketFactory.getSocketFactory();
    registry.register(new Scheme("http", plain, 80));
    registry.register(new Scheme("https", ssl, 443));

    connMgr = new ThreadSafeClientConnManager(params, registry);
    httpClient = new DefaultHttpClient(connMgr, params);
  }

  /**
   * Shuts down the SinglyHttpClient.  This method can be called to properly
   * release resources, such as open connections, that are being held.
   */
  public void shutdown() {
    httpClient.getConnectionManager().shutdown();
  }

  /**
   * Returns true if we can successfully connect to the url and get a 200 HTTP
   * response code.
   * 
   * @param url The url to ping.
   * 
   * @return True if we can successfully connect to the url.
   */
  public boolean ping(String url) {

    try {

      // must be able to connect and get the status code, but does not download
      // any of the page content
      HttpGet httpget = new HttpGet(url);
      HttpResponse response = httpClient.execute(httpget);
      StatusLine status = response.getStatusLine();
      int statusCode = status.getStatusCode();
      if (statusCode == 200) {
        httpget.abort();
        return true;
      }
    }
    catch (Exception e) {
      return false;
    }

    return false;
  }

  /**
   * Returns a byte array containing the GET response to the url.  This method
   * will try up to 3 times to retrieve a url before erroring.
   * 
   * @param url The url to GET.
   * 
   * @return The url content as a byte array.
   */
  public byte[] get(String url)
    throws HttpException {

    HttpGet httpget = null;
    boolean errored = false;

    // try a max number of time to get the url
    for (int i = 0; i < maxRetries; i++) {

      try {

        httpget = new HttpGet(url);
        HttpResponse response = httpClient.execute(httpget);
        StatusLine status = response.getStatusLine();
        int statusCode = status.getStatusCode();
        if (statusCode >= 300) {
          throw new HttpException(statusCode, status.getReasonPhrase());
        }

        // log success if previously errored
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          if (errored) {
            Log.w(TAG, "get succedded: " + url + " on attempt " + (i + 1));
          }
          return EntityUtils.toByteArray(entity);
        }

        return null;
      }
      catch (HttpException he) {
        httpget.abort();
        throw he;
      }
      catch (Exception e) {
        errored = true;
        Log.w(TAG, e.getMessage() + " on get: " + url + " retrying " + (i + 1));
        httpget.abort();
      }

    }

    throw new HttpException("Error getting url " + url + ", tried "
      + maxRetries);
  }

  /**
   * Returns a byte array containing the POST response to the url.
   * 
   * @param url The url to POST.
   * @param params The list of name and value pairs to post to the url.
   * 
   * @return The url content as a byte array.
   */
  public byte[] post(String url, List<NameValuePair> params)
    throws HttpException {

    try {
      UrlEncodedFormEntity body = new UrlEncodedFormEntity(params);
      return postAsBody(url, body);
    }
    catch (UnsupportedEncodingException e) {
      throw new HttpException(e);
    }
  }

  /**
   * Returns a byte array containing the POST response to the url.
   * 
   * @param url The url to POST.
   * @param body The raw body content to post to the url.
   * 
   * @return The url content as a byte array.
   */
  public byte[] postAsBody(String url, StringEntity body)
    throws HttpException {

    HttpPost httppost = null;
    try {

      httppost = new HttpPost(url);
      httppost.setEntity(body);

      HttpResponse response = httpClient.execute(httppost);
      StatusLine status = response.getStatusLine();
      int statusCode = status.getStatusCode();
      if (statusCode >= 300) {
        throw new HttpException(statusCode, status.getReasonPhrase());
      }

      HttpEntity entity = response.getEntity();
      if (entity != null) {
        return EntityUtils.toByteArray(entity);
      }

      return null;
    }
    catch (HttpException he) {
      httppost.abort();
      throw he;
    }
    catch (Exception e) {
      httppost.abort();
      throw new HttpException(e);
    }
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public void setConnectionTimeoutInSeconds(int connectionTimeoutInSeconds) {
    this.connectionTimeoutInSeconds = connectionTimeoutInSeconds;
  }

  public void setSocketTimeoutInSeconds(int socketTimeoutInSeconds) {
    this.socketTimeoutInSeconds = socketTimeoutInSeconds;
  }

  public void setTcpNoDelay(boolean tcpNoDelay) {
    this.tcpNoDelay = tcpNoDelay;
  }

  public void setSoLingerInSeconds(int soLingerInSeconds) {
    this.soLingerInSeconds = soLingerInSeconds;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

}
