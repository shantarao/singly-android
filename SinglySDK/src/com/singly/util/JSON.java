package com.singly.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSON {

  public static boolean looksLikeJson(String content) {
    return content.trim().startsWith("{");
  }

  public static JSONObject parse(String json) {
    try {
      JSONObject jsonObj = new JSONObject(json);
      return jsonObj;
    }
    catch (JSONException e) {
      return null;
    }
  }

  public static String getString(JSONObject parent, String name, String def) {
    String value = def;
    if (parent != null && !parent.isNull(name)) {
      try {
        value = parent.getString(name);
      }
      catch (JSONException e) {
        // do nothing, will return default value
      }
    }
    return value;
  }

  public static boolean getBoolean(JSONObject parent, String name, boolean def) {
    boolean value = def;
    if (parent != null && !parent.isNull(name)) {
      try {
        value = parent.getBoolean(name);
      }
      catch (JSONException e) {
        // do nothing, will return default value
      }
    }
    return value;
  }

  public static int getInt(JSONObject parent, String name, int def) {
    int value = def;
    if (parent != null && !parent.isNull(name)) {
      try {
        value = parent.getInt(name);
      }
      catch (JSONException e) {
        // do nothing, will return default value
      }
    }
    return value;
  }

  public static long getLong(JSONObject parent, String name, long def) {
    long value = def;
    if (parent != null && !parent.isNull(name)) {
      try {
        value = parent.getLong(name);
      }
      catch (JSONException e) {
        // do nothing, will return default value
      }
    }
    return value;
  }

  public static double getBoolean(JSONObject parent, String name, double def) {
    double value = def;
    if (parent != null && !parent.isNull(name)) {
      try {
        value = parent.getDouble(name);
      }
      catch (JSONException e) {
        // do nothing, will return default value
      }
    }
    return value;
  }

  public static Map<String, JSONObject> children(JSONObject node)
    throws IOException {

    Map<String, JSONObject> children = new LinkedHashMap<String, JSONObject>();
    Iterator<String> jsonIt = node.keys();
    while (jsonIt.hasNext()) {
      String name = jsonIt.next();
      try {
        JSONObject childObj = node.getJSONObject(name);
        children.put(name, childObj);
      }
      catch (JSONException e) {
        // do nothing, continue
      }
    }
    return children;
  }

  public static List<JSONObject> getValues(JSONObject parent, String name) {

    List<JSONObject> jsonObjs = new ArrayList<JSONObject>();
    try {
      JSONArray values = parent.getJSONArray(name);
      for (int i = 0; i < values.length(); i++) {
        try {
          if (values.isNull(i)) {
            jsonObjs.add(null);
          }
          else {
            jsonObjs.add(values.getJSONObject(i));
          }
        }
        catch (Exception e) {
          // error in getting index, set as null
          jsonObjs.add(null);
        }
      }
    }
    catch (Exception e) {
      // do nothing, will return empty array
    }

    return jsonObjs;
  }

  public static List<String> getStrings(JSONObject parent, String name) {

    List<String> strings = new ArrayList<String>();
    try {
      JSONArray values = parent.getJSONArray(name);
      for (int i = 0; i < values.length(); i++) {
        try {
          if (values.isNull(i)) {
            strings.add(null);
          }
          else {
            strings.add(values.getString(i));
          }
        }
        catch (Exception e) {
          // error in getting index, set as null
          strings.add(null);
        }
      }
    }
    catch (Exception e) {
      // do nothing, will return empty array
    }

    return strings;
  }

}
