package com.singly.android.component;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import com.singly.android.sdk.R;

/**
 * An activity class that combines the FriendsListFragment with the 
 * TableOfContentsFragment to display a list of friends from all services the user
 * is authenticated against along with a table of contents.
 * 
 * By default a Table of Contents showing A-Z and * is displayed on the right
 * hand side of the activity sized to fit the available area.  Touching a letter
 * will jump to the start of the letter in the FriendsListFragment.
 * 
 * Intent values can be passed in as intent extra information to change the
 * behavior of the FriendsListActivity component.  They are:
 * 
 * <ol>
 *   <li>displayTableOfContents - True or false to display a table or contents
 *   on the right side of the Activity.  Default is true.</li>
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
 *   <li>syncContacts - True or false to sync phone contacts to the api.</li>
 * </ol>
 * 
 * To use the FriendsListActivity you will want to create a subclass of this
 * activity and override the {@link #onFriendClick(AdapterView, View, int)}
 * method to add specific handling for when a Friend row is clicked.
 * 
 * When using the FriendsListActivity the following two activities must be 
 * registered in the AndroidManifest.xml file as follows.
 * 
 * <pre>
 * <activity android:name="com.singly.android.component.FriendsListActivity" />
 * <activity android:name="com.singly.android.component.DeviceOwnerActivity" />
 * </pre>
 * 
 * When syncing contacts, which is the default, the read contacts permission
 * must be specified in the AndroidManifest.xml file as follows.
 * 
 * <pre>
 * <uses-permission android:name="android.permission.READ_CONTACTS" />
 * </pre>
 * 
 * This allows reading from the contacts provider to get phone contacts to sync.
 */
public class FriendsListActivity
  extends FragmentActivity
  implements FriendsListRowClickListener, TableOfContentsTouchListener {

  protected LinearLayout singlyFriendsTableOfContentsLayout;
  protected LinearLayout singlyFriendsListLayout;
  protected FriendsListFragment friendsList;
  protected TableOfContentsFragment tableOfContents;
  protected boolean displayTableOfContents = true;

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

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.singly_friends_activity);

    // the id and layouts for the fragments
    int friendsListId = R.id.singlyFriendsListLayout;
    int friendsTocId = R.id.singlyFriendsTableOfContentsLayout;
    singlyFriendsListLayout = (LinearLayout)findViewById(friendsListId);
    singlyFriendsTableOfContentsLayout = (LinearLayout)findViewById(friendsTocId);

    Intent intent = getIntent();

    // get the block configuration for the adapter
    blocksToPreload = intent.getIntExtra("blocksToPreLoad", 1);
    blockSize = intent.getIntExtra("blockSize", 20);
    if (blockSize > 20) {
      blockSize = 20;
    }
    blocksToCache = intent.getIntExtra("blocksToCache", 50);

    // if showing images, setup the image cache, 2 parallel downloads, 200
    // images in memory
    displayImages = intent.getBooleanExtra("displayImages", true);
    imagesInParallel = intent.getIntExtra("imagesInParallel", 2);
    imageCacheSize = intent.getIntExtra("imageCacheSize", 200);
    imageCacheDir = intent.getStringExtra("imageCacheDir");

    // showing table of contents
    displayTableOfContents = intent.getBooleanExtra("displayTableOfContents",
      true);

    FragmentManager fragmentManager = getSupportFragmentManager();

    // setup the friends list fragment
    Fragment friendsListFrag = fragmentManager.findFragmentById(friendsListId);
    if (friendsListFrag != null) {
      friendsList = (FriendsListFragment)friendsListFrag;
    }
    else if (singlyFriendsListLayout != null) {

      friendsList = new FriendsListFragment();
      friendsList.setBlockSize(blockSize);
      friendsList.setBlocksToPreload(blocksToPreload);
      friendsList.setBlocksToCache(blocksToCache);

      if (displayImages) {
        friendsList.setDisplayImages(true);
        friendsList.setImagesInParallel(imagesInParallel);
        friendsList.setImageCacheSize(imageCacheSize);
        friendsList.setImageCacheDir(imageCacheDir);
      }

      FragmentTransaction friendsListTrans = fragmentManager.beginTransaction();
      friendsListTrans.add(friendsListId, friendsList);
      friendsListTrans.commit();
    }

    // setup the table of contents fragment
    Fragment tocFrag = fragmentManager.findFragmentById(friendsTocId);
    if (tocFrag != null) {
      tableOfContents = (TableOfContentsFragment)tocFrag;
    }
    else if (singlyFriendsTableOfContentsLayout != null
      && displayTableOfContents) {

      tableOfContents = new TableOfContentsFragment();
      FragmentTransaction tocTrans = fragmentManager.beginTransaction();
      tocTrans.add(friendsTocId, tableOfContents);
      tocTrans.commit();
    }
  }

  @Override
  public void onTableOfContentsTouched(String letter, int position) {
    friendsList.setSelection(position);
  }

  @Override
  public void onFriendClicked(Friend friend, int pos) {

    String name = friend != null ? friend.name : "Nobody";
    AlertDialog.Builder okCancelDialog = new AlertDialog.Builder(
      FriendsListActivity.this);
    okCancelDialog.setTitle(name);
    okCancelDialog.setMessage(name);
    okCancelDialog.setPositiveButton("OK", null);
    okCancelDialog.setNegativeButton("Cancel", null);
    okCancelDialog.show();

  }

}
