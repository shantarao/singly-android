package com.singly.android.util;

import android.graphics.Bitmap;

public class ImageInfo {

  public String id = null;
  public String imageUrl = null;
  public int width = -1;
  public int height = -1;
  public Bitmap.CompressFormat format;
  public int quality = 100;
  public boolean sample = true;
  public ImageCacheListener listener = null;

}
