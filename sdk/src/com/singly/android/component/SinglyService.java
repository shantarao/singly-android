package com.singly.android.component;

import java.util.Map;

import com.singly.android.util.ImageInfo;

/**
 * Holds information about a Singly service from the /services endpoint.
 */
public class SinglyService {
  String id;
  String name;
  Map<String, String> icons;
  ImageInfo imageInfo;
}
