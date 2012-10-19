package com.singly.android.util;

import android.graphics.Bitmap;

/**
 * A callback class used with {@link RemoteImageCache}.  Notifies when an image
 * has been downloaded and cached or failed.
 */
public class ImageCacheListener {

  /**
   * When an image has been successfully downloaded and cached.
   * 
   * When run through the RemoteImageCache this method is guaranteed to be run
   * in the UI thread.
   * 
   * @param imageInfo The info of the image.
   * @param bitmap The Bitmap of the image.
   */
  public void onSuccess(ImageInfo imageInfo, Bitmap bitmap) {

  }

  /**
   * When an image either failed download or could not be cached.
   * 
   * When run through the RemoteImageCache this method is guaranteed to be run
   * in the UI thread.
   * 
   * @param error The failure error.
   * @param imageInfo The info of the image.
   */
  public void onFailure(Throwable error, ImageInfo imageInfo) {

  }

}
