package com.singly.android.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.singly.android.client.AsyncApiResponseHandler;
import com.singly.android.client.SinglyClient;
import com.singly.android.client.SinglyClient.Authentication;
import com.singly.android.sdk.R;
import com.singly.android.util.JSON;
import com.singly.android.util.RemoteImageCache;

/**
 * An activity class that display a list of friends from all services the user
 * is authenticated against.  This Activity wraps the Singly /friends API.
 * 
 * The FriendsListActivity uses a number of optimizations to make the list 
 * display quickly and scroll smoothly while supporting lists of any size and 
 * allowing showing an image per friend.  First, an implementation of the 
 * {@link AbstractCachingBlockLoadedListAdapter} is used to load Friend rows in
 * blocks and to cache frequently used rows.  Second {@link RemoteImageCache}
 * is used to download images in background threads while limiting the nummber
 * of concurrent downloads and scaling the images to reduce memory consumption.
 * Third, a view holder pattern is used in the ListAdapter to reuses Views
 * instances in the ListView for smooth scrolling.
 * 
 * By default a Table of Contents showing A-Z and * is displayed on the right
 * hand side of the activity sized to fit the available area.  Touching a letter
 * will jump to the start of the letter in the Friends list.
 * 
 * Intent values can be passed in as intent extra information to change the
 * behavior of the FriendsListActivity component.  They are:
 * 
 * <ol>
 *   <li>includeTableOfContents - True or false to include a table or contents
 *   on the right side of the Activity.  Default is true.</li>
 *   <li>blockSize - The number of Friends loaded in a single block.  The 
 *   maximum is 20 as defined by the /friends API.</li>
 *   <li>blocksToCache - The number of blocks of blockSize to cache.  By default
 *   we cache 50 blocks or 1000 total rows.  Blocks are transparently reloaded
 *   when they are re-requested after being dropped from the cache.</li>
 *   <li>blocksToPreLoad - When a block is loaded, the number of blocks on 
 *   either side to load to support smooth forward and reverse scrolling.</li>
 *   <li>showImages - True or false, should images be downloaded and displayed
 *   for each Friend in the list.  A default image is displayed if an image 
 *   cannot be found or downloaded.</li>
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
 * When using the FriendsListActivity the following two activites must be 
 * registered in the AndroidManifest.xml file.
 * 
 * <pre>
 * <activity android:name="com.singly.android.component.FriendsListActivity" />
 * <activity android:name="com.singly.android.component.DeviceOwnerActivity" />
 * </pre>
 * 
 * When syncing contacts, which is the default, the read contacts permission
 * must be specified in the AndroidManifest.xml file.
 * 
 * <pre>
 * <uses-permission android:name="android.permission.READ_CONTACTS" />
 * </pre>
 * 
 * This allows reading from the contacts provider to get phone contacts to sync.
 */
public class FriendsListActivity
  extends Activity {

  protected RemoteImageCache imageCache;
  protected SinglyClient singlyClient;

  protected LinearLayout tableOfContentsLayout;
  protected ListView friendsListView;
  protected FriendsListAdapter friendsListAdapter;

  // block configuration
  protected int rows = 0;
  protected int blockSize = 20;
  protected int blocksToPreload = 2;
  protected int blocksToCache = 50;

  // image configuration
  protected boolean showImages = true;
  protected int imageCacheSize = 200;
  protected int imagesInParallel = 2;
  protected String imageCacheDir = null;

  /**
   * Syncs the contacts from the user's phone to the /friends/android api.
   */
  private void syncContactsFromPhone() {

    // prompt the user to enter a phone number as this seems to be the only
    // reliable way to get the device information, also works on non-phones
    // exit after, can't sync contacts unless we have owner info
    SharedPreferences prefs = getSharedPreferences("singly",
      Context.MODE_PRIVATE);
    String ownerName = prefs.getString(SinglyClient.OWNER_NAME, null);
    String ownerPhone = prefs.getString(SinglyClient.OWNER_PHONE_NUMBER, null);
    String ownerEmail = prefs.getString(SinglyClient.OWNER_EMAIL_ADDRESS, null);
    if (StringUtils.isBlank(ownerName) || StringUtils.isBlank(ownerPhone)
      || StringUtils.isBlank(ownerEmail)) {
      startActivityForResult(new Intent(this, DeviceOwnerActivity.class), 1);
      return;
    }

    // contacts list to sync
    List<Map<String, Object>> contacts = new ArrayList<Map<String, Object>>();

    // add the owner to the contact list
    Map<String, Object> owner = new LinkedHashMap<String, Object>();
    owner.put("self", true);
    owner.put("id", "00000");
    owner.put("name", ownerName);
    owner.put("phone", ownerPhone);
    owner.put("email", ownerEmail);
    contacts.add(owner);

    // get the content resolver and query for all contacts
    ContentResolver cr = getContentResolver();
    Cursor contactCur = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
      null, null, null);

    // loop through contacts if we have them
    if (contactCur.getCount() > 0) {
      while (contactCur.moveToNext()) {

        Map<String, Object> contact = new LinkedHashMap<String, Object>();

        // get the name and id of the contact
        String id = contactCur.getString(contactCur
          .getColumnIndex(ContactsContract.Contacts._ID));
        String name = contactCur.getString(contactCur
          .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

        contact.put("id", StringUtils.leftPad(id, 5, "0"));
        contact.put("name", name);

        // if there are phone numbers then get all phone numbers for the contact
        Cursor phoneCur = getContentResolver().query(
          ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
          ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null,
          null);
        boolean hasPhone = phoneCur.getCount() > 0;
        if (hasPhone) {
          List<String> phoneNumbers = new ArrayList<String>();
          while (phoneCur.moveToNext()) {
            String phoneNumber = phoneCur.getString(phoneCur
              .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            phoneNumbers.add(phoneNumber);
          }
          contact.put("phones", phoneNumbers);
        }
        phoneCur.close();

        // if there are emails addresses then get all emails for the contact
        Cursor emailCur = getContentResolver().query(
          ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
          ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + id, null,
          null);
        boolean hasEmail = emailCur.getCount() > 0;
        if (hasEmail) {
          List<String> emailAddresses = new ArrayList<String>();
          while (emailCur.moveToNext()) {
            String emailAddress = emailCur.getString(emailCur
              .getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
            emailAddresses.add(emailAddress);
          }
          contact.put("emails", emailAddresses);
        }
        emailCur.close();

        // must have either phone or email to sync contact
        if (hasPhone || hasEmail) {
          contacts.add(contact);
        }
      }
      contactCur.close();
    }

    // sync the contacts, post to /friends/android
    Map<String, String> qparams = new HashMap<String, String>();
    Authentication auth = singlyClient.getAuthentication(this);
    if (StringUtils.isNotBlank(auth.accessToken)) {
      qparams.put("access_token", auth.accessToken);
    }
    
    String contactsJson = JSON.serializeToJson(contacts);
    singlyClient.doBodyApiRequest(this, "/friends/android", qparams,
      contactsJson.getBytes(), "application/json",
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {
          Toast.makeText(FriendsListActivity.this, "Contacts synced",
            Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(Throwable error) {
          Toast.makeText(FriendsListActivity.this, "Error syncing contacts",
            Toast.LENGTH_SHORT).show();
        }
      });
  }

  /**
   * Displays the table of contents component if configured to do so.
   */
  private void displayTableOfContents() {

    // create the table of contents
    Intent intent = getIntent();
    if (intent.getBooleanExtra("includeTableOfContents", true)) {

      char[] letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ*".toCharArray();
      final String[] tableOfContents = new String[letters.length];
      for (int i = 0; i < letters.length; i++) {
        tableOfContents[i] = String.valueOf(letters[i]);
      }
      tableOfContentsLayout = (LinearLayout)findViewById(R.id.friendsTableOfContentsLayout);

      // create the table of contents
      final int tocHeight = tableOfContentsLayout.getHeight();
      final int tocLength = tableOfContents.length;
      final double pixelsPerItem = (double)tocHeight / tocLength;
      int fontSize = (int)(pixelsPerItem * 0.80d);

      // setup the keys for the table of contents
      for (String key : tableOfContents) {

        TextView letterTextView = new TextView(this);
        letterTextView.setText(String.valueOf(key));
        letterTextView.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL);
        letterTextView.setPadding(10, 0, 10, 0);
        letterTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
          LayoutParams.WRAP_CONTENT, 1);
        letterTextView.setLayoutParams(params);
        tableOfContentsLayout.addView(letterTextView);
      }

      // on touch passes through the to gesture detector
      tableOfContentsLayout.setOnTouchListener(new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

          if (event.getAction() == MotionEvent.ACTION_DOWN) {
            tableOfContentsLayout.setBackgroundColor(0xffbbbbbb);
          }
          else if (event.getAction() == MotionEvent.ACTION_UP) {
            tableOfContentsLayout.setBackgroundColor(0x88bbbbbb);
          }

          // get the index in the toc that was touched
          int tocIndex = (int)(event.getY() / pixelsPerItem);
          tocIndex = Math.min(Math.max(tocIndex, 0), tocLength - 1);

          // get the letter in the toc in the index and from that the position
          // that starts that letter, the jump to that position in the list
          int position = friendsListAdapter
            .getTableOfContentsPosition(tableOfContents[tocIndex]);
          if (position >= 0) {
            friendsListView.setSelection(position);
          }

          return true;
        }
      });
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.singly_friends);

    final Intent intent = getIntent();

    // get the singly client
    this.singlyClient = SinglyClient.getInstance();

    // get the block configuration for the adapter
    blocksToPreload = intent.getIntExtra("blocksToPreLoad", 1);
    blockSize = intent.getIntExtra("blockSize", 20);
    if (blockSize > 20) {
      blockSize = 20;
    }
    blocksToCache = intent.getIntExtra("blocksToCache", 50);

    // sync contacts from phone if desired
    boolean syncContacts = intent.getBooleanExtra("syncContacts", true);
    if (syncContacts) {
      syncContactsFromPhone();
    }

    // create the friends list view
    friendsListView = (ListView)findViewById(R.id.friendsListView);

    // if showing images, setup the image cache, 2 parallel downloads, 200
    // images in memory
    showImages = intent.getBooleanExtra("showImages", true);
    if (showImages) {
      imagesInParallel = intent.getIntExtra("imagesInParallel", 2);
      imageCacheSize = intent.getIntExtra("imageCacheSize", 200);
      imageCacheDir = intent.getStringExtra("imageCacheDir");
      this.imageCache = new RemoteImageCache(this, imagesInParallel,
        imageCacheDir, imageCacheSize);
    }

    // get the access token and query parameters
    Map<String, String> qparams = new HashMap<String, String>();
    Authentication auth = singlyClient.getAuthentication(this);
    qparams.put("access_token", auth.accessToken);

    // get total number of rows
    singlyClient.doGetApiRequest(this, "/friends", qparams,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {

          // get the number of friends from the friends API
          JsonNode root = JSON.parse(response);
          rows = JSON.getInt(root, "all");

          // create the friends adapter and set into the view
          friendsListAdapter = new FriendsListAdapter(FriendsListActivity.this,
            rows, blockSize, blocksToPreload, blocksToCache, imageCache);
          friendsListView.setAdapter(friendsListAdapter);

          // handle clicks on friend rows in the friends list view
          friendsListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View item, int pos,
              long id) {

              // passes of to subclass methods
              onFriendClick(parent, item, pos);
            }
          });

          displayTableOfContents();
        }

        @Override
        public void onFailure(Throwable error) {
        }
      });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    imageCache.shutdown();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    // returned from the DeviceOwnerActivity, try and sync the contacts
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == 0) {
      syncContactsFromPhone();
    }
  }

  /**
   * Simple implementation showing which row was clicked.  Subclasses will want
   * to override this method to provide specific functionality when a given row
   * of a friend is clicked.
   * 
   * @param parent The parent ListView.
   * @param item The row View clicked.
   * @param pos The position of the row that was clicked.
   */
  protected void onFriendClick(AdapterView<?> parent, View item, int pos) {

    Friend friend = friendsListAdapter.getBackingObject(pos);

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
