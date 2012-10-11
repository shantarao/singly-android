package com.singly.android.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIUtils;

/**
 * Utility methods for creating and manipulating URLs.
 */
public class URLUtils {

  private static Pattern IP_PATTERN = Pattern
    .compile("(\\d{1,3}\\.){3}(\\d{1,3})");

  private static final String PARAMETER_SEPARATOR = "&";
  private static final String NAME_VALUE_SEPARATOR = "=";

  /**
   * Creates a url using the base singly api url, the path, and the query
   * parameters specified.
   * 
   * The url is assumed to be in UTF-8 format.  The query parameters are
   * not required.
   * 
   * @param scheme The url scheme.
   * @param host The url hostname.
   * @param path The url path.
   * @param qparams The optional url query parameters.
   * 
   * @return A formatted, UTF-8 singly url string.
   */
  public static String createURL(String scheme, String host, String path,
    Map<String, String> parameters)
    throws URISyntaxException {

    // query parameters are optional
    String query = null;
    if (parameters != null && parameters.size() > 0) {
      query = URLUtils.toQueryString(parameters, "UTF-8");
    }

    // create the formatted UTF-8 url
    URI uri = URIUtils.createURI(scheme, host, -1, path, query, null);

    return uri.toASCIIString();
  }

  /**
   * Format the url parameters into a query string with the given encoding.
   * 
   * @param parameters A map of the query parameters to add.
   * @param encoding The encoding of the parameters.
   * 
   * @return A formatted query string.
   */
  public static String toQueryString(Map<String, String> parameters,
    String encoding) {

    final StringBuilder result = new StringBuilder();
    for (Map.Entry<String, String> parameter : parameters.entrySet()) {
      final String encodedName = encode(parameter.getKey(), encoding);
      final String value = parameter.getValue();
      final String encodedValue = value != null ? encode(value, encoding) : "";
      if (result.length() > 0) {
        result.append(PARAMETER_SEPARATOR);
      }
      result.append(encodedName);
      result.append(NAME_VALUE_SEPARATOR);
      result.append(encodedValue);
    }
    return result.toString();
  }

  /**
   * Decode the URL encoded content.
   * 
   * @param content The content to decode.
   * @param charset The character set.  Defaults to UTF-8.
   * 
   * @return The decoded content.
   */
  public static String decode(String content, String charset) {
    try {
      return URLDecoder.decode(content, charset != null ? charset : "UTF-8");
    }
    catch (UnsupportedEncodingException problem) {
      throw new IllegalArgumentException(problem);
    }
  }

  /**
   * Encode the content to URL encoding.
   * 
   * @param content The content to encode.
   * @param charset The character set.  Defaults to UTF-8.
   * 
   * @return The encoded content.
   */
  public static String encode(String content, String charset) {
    try {
      return URLEncoder.encode(content, charset != null ? charset : "UTF-8");
    }
    catch (UnsupportedEncodingException problem) {
      throw new IllegalArgumentException(problem);
    }
  }

  /**
   * Returns an array of the segments of a hostname for a url.
   * 
   * @param url The url to get the host segments for.
   * 
   * @return An array of hostname segments for the url.
   */
  public static String[] getHostSegments(URL url) {
    String host = url.getHost();
    if (IP_PATTERN.matcher(host).matches())
      return new String[] {
        host
      };
    return host.split("\\.");
  }
  
  /**
   * Returns an array of the segments of a hostname for a url.
   * 
   * @param url The String url to get the host segments for.
   * 
   * @return An array of hostname segments for the url.
   */
  public static String[] getHostSegments(String url)
    throws MalformedURLException {
    return getHostSegments(new URL(url));
  }

  /**
   * Returns the hostname for the url.
   * 
   * @param url The starting url.
   * 
   * @return The url hostname.
   */
  public static String getHost(String url) {
    try {
      return new URL(url).getHost().toLowerCase();
    }
    catch (MalformedURLException e) {
      return null;
    }
  }

  /**
   * Returns the page of the url with the query string removed.
   * 
   * @param url The starting url.
   * 
   * @return The url with the query string removed.
   */
  public static String getPage(String url) {
    try {
      url = url.toLowerCase();
      String queryStr = new URL(url).getQuery();
      return (queryStr != null) ? url.replace("?" + queryStr, "") : url;
    }
    catch (MalformedURLException e) {
      return null;
    }
  }

  /**
   * Returns the protocol of the url.
   * 
   * @param url The starting url.
   * 
   * @return The url protocol.
   */
  public static String getProtocol(String url) {
    try {
      url = url.toLowerCase();
      String protocolStr = new URL(url).getProtocol();
      return protocolStr;
    }
    catch (MalformedURLException e) {
      return null;
    }
  }

  /**
   * Returns a String version of the URL with the protocol removed.
   * 
   * @param url The starting url.
   * 
   * @return The url with the protocol removed.
   */
  public static String stripProtocol(String url) {
    try {
      url = url.toLowerCase();
      String protocolStr = new URL(url).getProtocol();
      return (protocolStr != null) ? url.replace(protocolStr + "://", "") : url;
    }
    catch (MalformedURLException e) {
      return null;
    }
  }

  public static String getExtension(String filename) {
    int extIndex = StringUtils.lastIndexOf(filename, ".");
    return extIndex > 0 && extIndex + 1 < filename.length() ? StringUtils
      .substring(filename, extIndex + 1) : null;
  }
}
