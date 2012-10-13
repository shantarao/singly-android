package com.singly.android.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;

/**
 * A class that downloads and caches remote images to the local filesystem.
 * 
 * This class handles downloading images in background threads, limiting the
 * number of concurrent downloads, sampling the images to the current sample 
 * rate for the size at which the image will be displayed, writing the sampled
 * image to local disk, and running callbacks in the main UI thread when the
 * entire process is complete or has errored.
 * 
 * The class consists of two layers of caching, in memory and on disk.  If an
 * image is already stored on the local disk it will be pulled into memory upon
 * use.  If it hasn't been downloaded, it will be downloaded in a background
 * thread, stored on local disk, and then brought into memory.  If an image has
 * been recently used it will most likely be in memory and pulled from the in
 * memory cache for quick access.
 * 
 * Developers should be able to drop this class in and not have to worry about
 * if the image is remote, has already been cached locally, or is in memory.
 * There are options for specifying max concurrent downloads and max number of
 * images to cache in memory.
 */
public class RemoteImageCache {

  public static final String DEFAULT_CACHE_DIR = "_image_cache_";
  
  private static final String POISON_PILL = "_shutdown_";

  private File storageDir;

  private AtomicBoolean active = new AtomicBoolean(false);
  private final Semaphore throttle;
  private BlockingQueue<ImageInfo> queue = new LinkedBlockingQueue<ImageInfo>();

  private LruCache<String, Bitmap> imageCache;
  private Set<String> bad = Collections.synchronizedSet(new HashSet<String>());
  private Set<String> down = Collections.synchronizedSet(new HashSet<String>());

  private AsyncHttpClient httpClient = new AsyncHttpClient();
  private Handler handler = new Handler(Looper.getMainLooper());

  /**
   * Downloader thread that reads from a queue and downloads remote images to
   * local storage.  Runs continuously while the 
   */
  private class DownloaderThread
    extends Thread {

    public void run() {

      // continue downloading while not shutdown
      while (active.get()) {

        ImageInfo nextImage = null;
        try {
          
          // wait until an image to download appears in the queue
          nextImage = queue.take();
          
          // poison pill for clean shutdown of blocking queue
          if (nextImage.id.equals(POISON_PILL)) {
            continue;
          }
        }
        catch (InterruptedException ie) {
          continue;
        }
        
        // acquire the semaphore to start downloading
        try {
          throttle.acquire();
        }
        catch (InterruptedException ie) {
          continue;
        }

        // download the image
        final ImageInfo imageInfo = nextImage;
        final File imageFile = new File(storageDir, imageInfo.id + ".img");

        httpClient.get(imageInfo.imageUrl, null,
          new BinaryHttpResponseHandler() {

            @Override
            public void onSuccess(byte[] bytes) {

              // image downloaded, release semaphore, let the next one go
              throttle.release();

              byte[] imageBytes = null;
              Bitmap bitmap = null;

              try {

                // if we are sampling, then sample the image and turn it into a
                // Bitmap, if not just turn it into a Bitmap
                if (imageInfo.sample) {

                  bitmap = BitmapUtils.decodeAndScaleImage(bytes,
                    imageInfo.width, imageInfo.height);
                  ByteArrayOutputStream baos = new ByteArrayOutputStream();
                  bitmap.compress(imageInfo.format, imageInfo.quality, baos);
                  imageBytes = baos.toByteArray();
                }
                else {

                  imageBytes = bytes;
                  bitmap = BitmapFactory.decodeByteArray(imageBytes, 0,
                    imageBytes.length);
                }

                // write the Bitmap bytes to local storage
                if (bitmap != null && imageBytes != null) {
                  FileUtils.writeByteArrayToFile(imageFile, imageBytes);
                  imageCache.put(imageInfo.id, bitmap);
                }
              }
              catch (Exception e) {
                // error converting bytes to image
              }
              finally {                
                // remove image from the downloading state
                down.remove(imageInfo.id);
              }

              // create a handler to ensure the callback listener runs in the
              // main UI thread, pass in the Bitmap and the original image info
              final Bitmap image = bitmap;
              handler.post(new Runnable() {

                @Override
                public void run() {
                  
                  // run the ImageCacheListner callback for success
                  if (imageInfo.listener != null) {
                    imageInfo.listener.onSuccess(imageInfo, image);
                  }
                }
              });
            }

            @Override
            public void onFailure(final Throwable error, byte[] bytes) {

              // image download failed, release semaphore, let the next one go
              throttle.release();
              
              // remove from the downloading state, add to bad images so we 
              // won't try to download again
              bad.add(imageInfo.id);
              down.remove(imageInfo.id);
              
              // create a handler to ensure the callback listener runs in the
              // main UI thread, pass in the error and the original image info
              handler.post(new Runnable() {

                @Override
                public void run() {
                  
                  // run the ImageCacheListner callback for failure
                  if (imageInfo.listener != null) {
                    imageInfo.listener.onFailure(error, imageInfo);
                  }
                }
              });
            }
          });
      }
    }
  }

  /**
   * Default constructor.
   * 
   * @param context The current Android context.
   * @param maxParallelDown The maximum number of downloads that can
   * happen concurrently.
   * @param cacheDir The local directory to store cached images. This is just
   * a directory name.  The directory will always be inside the applications 
   * data/files directory.
   * @param cacheSize The number of images to cache in memory.
   */
  public RemoteImageCache(Context context, int maxParallelDown,
    String cacheDir, int cacheSize) {

    // Context and LRUCache for in memory Bitmaps
    Context appContext = context.getApplicationContext();
    File dataDir = appContext.getFilesDir();
    this.storageDir = new File(dataDir, cacheDir != null ? cacheDir
      : DEFAULT_CACHE_DIR);
    this.imageCache = new LruCache<String, Bitmap>(cacheSize);

    // semaphore to limit parallel downloads
    this.throttle = new Semaphore(maxParallelDown, true);
    this.active.set(true);

    // downloader thread that pulls from a queue
    DownloaderThread downloaderThread = new DownloaderThread();
    downloaderThread.setDaemon(true);
    downloaderThread.start();
  }

  /**
   * Return the Bitmap of the image or null if the image is not available, 
   * either because it could not be downloaded or because it is currently
   * downloading.
   * 
   * @param imageInfo The image information.
   * 
   * @return A Bitmap of the image.
   */
  public Bitmap getImage(ImageInfo imageInfo) {

    // no image info or image url or bad image don't bother further
    if (imageInfo == null || imageInfo.imageUrl == null
      || bad.contains(imageInfo.id)) {
      return null;
    }

    // try and get from the cache
    Bitmap image = imageCache.get(imageInfo.id);
    if (image != null) {
      return image;
    }

    // if the file exists then read the file from internal storage and turn
    // it into an Bitmap
    File imageFile = new File(storageDir, imageInfo.id + ".img");
    if (imageFile.exists()) {

      // try and first get from local storage
      try {

        byte[] imageBytes = FileUtils.readFileToByteArray(imageFile);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0,
          imageBytes.length);
        imageCache.put(imageInfo.id, bitmap);

        return bitmap;
      }
      catch (IOException e) {
      }
    }

    // two step synchronized check and add for downloading state
    synchronized (down) {
      if (down.contains(imageInfo.id)) {
        return null;
      }
      down.add(imageInfo.id);
    }

    // drop into the download queue
    try {
      queue.put(imageInfo);
    }
    catch (InterruptedException ie) {
      // shouldn't happen, the queue is non blocking
    }

    return null;
  }

  /**
   * Shutdown the image cache, evicts all in memory Bitmaps, stops downloader
   * from running any future downloads.  Anything on the download queue or 
   * anything added to the download queue after shutdown is called is lost.
   */
  public void shutdown() {
    
    active.set(false);
    imageCache.evictAll();
    
    try {
      ImageInfo poisonPill = new ImageInfo();
      poisonPill.id = POISON_PILL;
      queue.put(poisonPill);
    }
    catch (InterruptedException ie) {
      // shouldn't happen, the queue is non blocking
    }
  }
}
