package com.singly.android.component;

import java.util.Map;

/**
 * An object that represents a Friend type in the Singly /friends API.
 */
public class Friend {

  public String handle;
  public String email;
  public String phone;
  public String service;
  public String name;
  public String description;
  public String imageUrl;
  public String profileUrl;
  public Map<String, Service> services;
  
  public static class Service {
    public String id;
    public String entry;
    public String url;
  }

}
