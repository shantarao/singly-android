package com.singly.android.component;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.singly.android.client.AsyncApiResponseHandler;
import com.singly.android.client.SinglyClient;
import com.singly.android.client.SinglyClient.Authentication;
import com.singly.android.sdk.R;
import com.singly.android.util.JSON;
import com.singly.android.util.RemoteImageCache;

/**
 * A Fragment component that displays a list of friends from all services the
 * user is authenticated against.  This Fragment wraps the Singly /friends API.
 * 
 * The FriendsListFragment uses a number of optimizations to make the list 
 * display quickly and scroll smoothly while supporting lists of any size and 
 * allowing showing an image per friend.  First, an implementation of the 
 * {@link AbstractCachingBlockLoadedListAdapter} is used to load Friend rows in
 * blocks and to cache frequently used rows.  Second {@link RemoteImageCache}
 * is used to download images in background threads while limiting the nummber
 * of concurrent downloads and scaling the images to reduce memory consumption.
 * Third, a view holder pattern is used in the ListAdapter to reuses Views
 * instances in the ListView for smooth scrolling.
 * 
 * The behavior of the FriendsListFragment can be configured as follows:
 * 
 * <ol>
 *   <li>blockSize - The number of Friends loaded in a single block.  The 
 *   maximum is 20 as defined by the /friends API.</li>
 *   <li>blocksToCache - The number of blocks of blockSize to cache.  By default
 *   we cache 50 blocks or 1000 total rows.  Blocks are transparently reloaded
 *   when they are re-requested after being dropped from the cache.</li>
 *   <li>blocksToPreLoad - When a block is loaded, the number of blocks on 
 *   either side to load to support smooth forward and reverse scrolling.</li>
 *   <li>displayImages - True or false, should images be downloaded and
 *   displayed for each Friend in the list.  A default image is displayed if
 *   an image cannot be found or downloaded.</li>
 *   <li>imageCacheSize - The number of images to cache in memory</li>
 *   <li>imageCacheDir - The image cache directory inside app data/files</li>
 *   <li>imagesInParallel - The max images to download in parallel.</li>
 * </ol>
 * 
 * To use the FriendsListFragment you will want to add it to an Activity. The 
 * parent activity can implement {@link FriendsListRowClickListener} to handle 
 * when a given row in the FriendsListFragment is clicked.
 */
public class FriendsListFragment
  extends Fragment {

  protected Activity activity;
  protected LinearLayout friendsLayout;
  protected ListView friendsListView;
  protected FriendsListAdapter friendsListAdapter;
  protected SinglyClient singlyClient;

  // block configuration
  protected int rows = 0;
  protected int blockSize = 20;
  protected int blocksToPreload = 2;
  protected int blocksToCache = 50;

  // image configuration
  protected boolean displayImages = true;
  protected int imageCacheSize = 200;
  protected int imagesInParallel = 2;
  protected String imageCacheDir = null;
  protected RemoteImageCache remoteImageCache;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    this.activity = activity;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {

    super.onCreateView(inflater, container, savedInstanceState);

    // create the friends list view
    friendsLayout = (LinearLayout)inflater.inflate(
      R.layout.singly_friends_list_fragment, container, false);
    friendsListView = (ListView)friendsLayout
      .findViewById(R.id.singlyFriendsListView);

    // get the singly client
    this.singlyClient = SinglyClient.getInstance();

    // get the access token and query parameters
    Map<String, String> qparams = new HashMap<String, String>();
    Authentication auth = singlyClient.getAuthentication(activity);
    qparams.put("access_token", auth.accessToken);

    // get total number of rows
    singlyClient.doGetApiRequest(activity, "/friends", qparams,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {

          // get the number of friends from the friends API
          JsonNode root = JSON.parse(response);
          rows = JSON.getInt(root, "all");

          // create the friends adapter and set into the view
          friendsListAdapter = new FriendsListAdapter(activity, rows,
            blockSize, blocksToPreload, blocksToCache);

          // if showing images, setup the image cache, 2 parallel downloads, 200
          // images in memory
          if (displayImages) {
            remoteImageCache = new RemoteImageCache(activity, imagesInParallel,
              imageCacheDir, imageCacheSize);
            friendsListAdapter.setDisplayImages(true);
            friendsListAdapter.setRemoteImageCache(remoteImageCache);
          }

          friendsListView.setAdapter(friendsListAdapter);

          // handle clicks on friend rows in the friends list view
          friendsListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View item, int pos,
              long id) {
              if (activity != null
                && activity instanceof FriendsListRowClickListener) {
                Friend friend = friendsListAdapter.getBackingObject(pos);
                ((FriendsListRowClickListener)activity).onFriendClicked(friend,
                  pos);
              }
            }
          });
        }

        @Override
        public void onFailure(Throwable error, String message) {
          Log.e(FriendsListFragment.class.getSimpleName(),
            "Error getting friends", error);
        }
      });

    return friendsLayout;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (remoteImageCache != null) {
      remoteImageCache.shutdown();
    }
  }

  public void setSelection(int position) {
    friendsListView.setSelection(position);
  }

  public int getBlockSize() {
    return blockSize;
  }

  public void setBlockSize(int blockSize) {
    this.blockSize = blockSize;
  }

  public int getBlocksToPreload() {
    return blocksToPreload;
  }

  public void setBlocksToPreload(int blocksToPreload) {
    this.blocksToPreload = blocksToPreload;
  }

  public int getBlocksToCache() {
    return blocksToCache;
  }

  public void setBlocksToCache(int blocksToCache) {
    this.blocksToCache = blocksToCache;
  }

  public boolean isDisplayImages() {
    return displayImages;
  }

  public void setDisplayImages(boolean displayImages) {
    this.displayImages = displayImages;
  }

  public int getImageCacheSize() {
    return imageCacheSize;
  }

  public void setImageCacheSize(int imageCacheSize) {
    this.imageCacheSize = imageCacheSize;
  }

  public int getImagesInParallel() {
    return imagesInParallel;
  }

  public void setImagesInParallel(int imagesInParallel) {
    this.imagesInParallel = imagesInParallel;
  }

  public String getImageCacheDir() {
    return imageCacheDir;
  }

  public void setImageCacheDir(String imageCacheDir) {
    this.imageCacheDir = imageCacheDir;
  }

}
