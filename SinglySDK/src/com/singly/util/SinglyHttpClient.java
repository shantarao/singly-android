package com.singly.util;

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

  public void shutdown() {
    httpClient.getConnectionManager().shutdown();
  }

  public boolean ping(String url) {

    try {

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
  
  public byte[] get(String url)
    throws HttpException {

    HttpGet httpget = null;
    boolean errored = false;

    for (int i = 0; i < maxRetries; i++) {

      try {

        httpget = new HttpGet(url);
        HttpResponse response = httpClient.execute(httpget);
        StatusLine status = response.getStatusLine();
        int statusCode = status.getStatusCode();
        if (statusCode >= 300) {
          throw new HttpException(statusCode, status.getReasonPhrase());
        }

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
