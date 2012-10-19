package com.singly.android.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Utility methods for working with Bitmap instances.
 */
public class BitmapUtils {

  /**
   * Determines the best sample rate for the Bitmap for the given width and 
   * height.  Then decodes the Bitmap using that sample rate.  Note that the 
   * Bitmap isn't returned at the width and height, it is returned using a
   * sampling rate that is appropriate for the desired width and height as 
   * Android will automatically scale a Bitmap to a given component area.
   * 
   * Decoding happens twice this method.  Once for getting the scale and once
   * for decoding at scale.  Because of this it is best to serialize the Bitmap
   * once it is decoded at scale to prevent double decoding on each load.
   * 
   * @param bytes The image bytes to be loaded into a Bitmap.
   * @param width The desired max width.
   * @param height The desired max height.
   * 
   * @return
   */
  public static Bitmap decodeAndScaleImage(byte[] bytes, int width, int height) {

    try {

      // decode the image file
      BitmapFactory.Options scaleOptions = new BitmapFactory.Options();
      scaleOptions.inJustDecodeBounds = true;
      BitmapFactory.decodeByteArray(bytes, 0, bytes.length, scaleOptions);

      // find the correct scale value as a power of 2.
      int scale = 1;
      while (scaleOptions.outWidth / scale / 2 >= width
        && scaleOptions.outHeight / scale / 2 >= height) {
        scale *= 2;
      }

      // decode with the sample size
      BitmapFactory.Options outOptions = new BitmapFactory.Options();
      outOptions.inSampleSize = scale;
      return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, outOptions);
    }
    catch (Exception e) {
    }

    return null;
  }

}
