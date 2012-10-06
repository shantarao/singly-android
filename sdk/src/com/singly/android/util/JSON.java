package com.singly.android.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility methods for JSON parsing and extraction of values from JSON nodes.
 */
public class JSON {

  /**
   * Returns true if the string look like a valid JSON string, starting and
   * ending with either squiggly or square brackets.
   * 
   * @param content The JSON string to check.
   * 
   * @return True if the string looks like a valid JSON string.
   */
  public static boolean looksLikeJson(String content) {
    
    String trimmed = content.trim();
    boolean squiggs = trimmed.startsWith("{") && trimmed.endsWith("}");
    boolean square = trimmed.startsWith("[") && trimmed.endsWith("]");
    
    return squiggs || square;
  }

  /**
   * Parses the JSON string into a JSONObject.  Returns null if the string
   * cannot be parsed.
   * 
   * @param json The JSON string to parse.
   * 
   * @return The parsed JSONObject or null if the string cannot be parsed.
   */
  public static JSONObject parse(String json) {
    try {
      JSONObject jsonObj = new JSONObject(json);
      return jsonObj;
    }
    catch (JSONException e) {
      return null;
    }
  }

  /**
   * Returns the String value from the field with the given name in the parent 
   * or the default String if the field doesn't exist or it cannot be coerced
   * into a String.
   * 
   * @param parent The parent from which to get the field.
   * @param name The name of the field to get.
   * @param def The default String value.
   * 
   * @return The String value from the field or the default String value if 
   * the field doesn't exist or can't be coerced.
   */
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

  /**
   * Returns the boolean value from the field with the given name in the parent 
   * or the default boolean if the field doesn't exist or it cannot be coerced
   * into a boolean.
   * 
   * @param parent The parent from which to get the field.
   * @param name The name of the field to get.
   * @param def The default boolean value.
   * 
   * @return The boolean value from the field or the default boolean value if 
   * the field doesn't exist or can't be coerced.
   */
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

  /**
   * Returns the int value from the field with the given name in the parent 
   * or the default int if the field doesn't exist or it cannot be coerced
   * into a int.
   * 
   * @param parent The parent from which to get the field.
   * @param name The name of the field to get.
   * @param def The default int value.
   * 
   * @return The int value from the field or the default int value if 
   * the field doesn't exist or can't be coerced.
   */
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

  /**
   * Returns the long value from the field with the given name in the parent 
   * or the default long if the field doesn't exist or it cannot be coerced
   * into a long.
   * 
   * @param parent The parent from which to get the field.
   * @param name The name of the field to get.
   * @param def The default long value.
   * 
   * @return The long value from the field or the default long value if 
   * the field doesn't exist or can't be coerced.
   */
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

  /**
   * Returns the double value from the field with the given name in the parent 
   * or the default double if the field doesn't exist or it cannot be coerced
   * into a double.
   * 
   * @param parent The parent from which to get the field.
   * @param name The name of the field to get.
   * @param def The default double value.
   * 
   * @return The double value from the field or the default double value if 
   * the field doesn't exist or can't be coerced.
   */
  public static double getDouble(JSONObject parent, String name, double def) {
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

  /**
   * Gets the fields from the JSONObject node and turns them into a Map of name 
   * to JSONObject values.
   * 
   * The Map is a LinkedHashMap returned implementation.  Children are returned
   * in the order in which they appear.
   * 
   * @param node The node from which to retrieve the fields.
   * 
   * @return A map of name to JSONObject values for the fields of the node.
   */
  public static Map<String, JSONObject> children(JSONObject node) {

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

  /**
   * Gets the values of the field with the given name in the parent as a List 
   * of JSONObject values.
   * 
   * The field value is assumed to be a JSON array.  If the value is not an
   * array then an empty list is returned.  Null values or values that cause an 
   * exception are set to null in the List.  
   * 
   * @param parent The parent from which to get the field.
   * @param name The name of the field to get.
   * 
   * @return A List of JSONObject values representing the JSON array at the
   * field in the parent or an empty List is the field is not an array.
   */
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

  /**
   * Gets the values of the field with the given name in the parent as a List 
   * of String values.
   * 
   * The field value is assumed to be a JSON array.  If the value is not an
   * array then an empty list is returned.  Null values or values that cause an 
   * exception are set to null in the List.  
   * 
   * @param parent The parent from which to get the field.
   * @param name The name of the field to get.
   * 
   * @return A List of String values representing the JSON array at the
   * field in the parent or an empty List is the field is not an array.
   */
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
