package com.singly.android.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.singly.android.client.AsyncApiResponseHandler;
import com.singly.android.client.SinglyClient;
import com.singly.android.client.SinglyClient.Authentication;
import com.singly.android.sdk.R;
import com.singly.android.util.ImageCacheListener;
import com.singly.android.util.ImageInfo;
import com.singly.android.util.JSON;
import com.singly.android.util.RemoteImageCache;

/**
 * A {@link AbstractCachingBlockLoadedListAdapter} implementation customized
 * for use with the Singly Friends API.
 */
public class FriendsListAdapter
  extends AbstractCachingBlockLoadedListAdapter<Friend> {

  private LayoutInflater inflater;
  private Context context;
  private SinglyClient singlyClient;
  private String accessToken;
  private Bitmap defaultImage;
  private Map<Integer, String> sectionPositions;

  private boolean displaySectionHeaders = true;
  private boolean displayImages = true;
  private int defaultImageResource = R.drawable.friend_noimage;
  private RemoteImageCache remoteImageCache;

  private static class ViewHolder {
    TextView sectionHeader;
    ProgressBar progress;
    TextView name;
    ImageView image;
  }

  @Override
  protected void loadBlock(final int blockId, final int offset, final int limit) {

    // get the access token and query parameters
    Map<String, String> qparams = new HashMap<String, String>();
    qparams.put("access_token", accessToken);
    qparams.put("offset", String.valueOf(offset));
    qparams.put("limit", String.valueOf(limit));

    // we only need the toc on the first call
    qparams.put("toc", sectionPositions == null ? "true" : "false");

    // make a call to the api to get the block
    singlyClient.doGetApiRequest(context, "/friends/all", qparams,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {

          List<Friend> blockOfFriends = new ArrayList<Friend>();
          JsonNode root = JSON.parse(response);

          for (JsonNode node : root) {

            if (sectionPositions == null) {

              // create the section headers from the table of contents
              sectionPositions = Collections
                .synchronizedMap(new HashMap<Integer, String>());
              Map<String, JsonNode> tocFields = JSON.getFields(node);
              for (Map.Entry<String, JsonNode> tocField : tocFields.entrySet()) {
                String tocKey = tocField.getKey();
                JsonNode tocNode = tocField.getValue();
                if (!StringUtils.equals(tocKey, "meta")) {
                  sectionPositions.put(JSON.getInt(tocNode, "offset"),
                    StringUtils.upperCase(tocKey));
                }
              }
            }
            else {

              // parse the friend
              Friend friend = new Friend();
              friend.name = JSON.getString(node, "name");
              friend.imageUrl = JSON.getString(node, "thumbnail_url");
              friend.handle = JSON.getString(node, "handle");
              friend.description = JSON.getString(node, "description");
              friend.email = JSON.getString(node, "email");
              friend.phone = JSON.getString(node, "phone");

              // parse the friend services
              JsonNode servicesN = JSON.getJsonNode(node, "services");
              Map<String, JsonNode> serviceFields = JSON.getFields(servicesN);
              Map<String, Friend.Service> services = new LinkedHashMap<String, Friend.Service>();
              for (Map.Entry<String, JsonNode> entry : serviceFields.entrySet()) {
                Friend.Service service = new Friend.Service();
                service.id = JSON.getString(node, "id");
                service.entry = JSON.getString(node, "entry");
                service.url = JSON.getString(node, "url");
                services.put(entry.getKey(), service);
              }
              friend.services = services;

              // add friend to the block
              blockOfFriends.add(friend);
            }
          }

          // cache the block
          finishAndCacheBlock(blockId, blockOfFriends);
        }

        @Override
        public void onFailure(Throwable error, String message) {
          error.printStackTrace();
        }
      });
  }

  /**
   * Default constructor.
   * 
   * @param rows The number of total rows in this adapter, for this list.
   * @param blockSize The number of rows per block.  Total blocks will be the
   * number of rows / blockSize, plus 1 block if remainder.
   * @param blocksToPreload The number of blocks to preload when a single block
   * is loaded into the cache.  This supports smooth reverse and forward scroll.
   * If the blockSize is low the blocksToPreload should be greater.
   * @param blocksToCache The number of blocks to keep in memory.
   */
  public FriendsListAdapter(Context context, int rows, int blockSize,
    int blocksToPreload, int blocksToCache) {

    super(rows, blockSize, blocksToPreload, blocksToCache);
    this.context = context;
    this.inflater = (LayoutInflater)context
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    // get the singly client and access token
    this.singlyClient = SinglyClient.getInstance();
    Authentication auth = singlyClient.getAuthentication(context);
    this.accessToken = auth.accessToken;
  }

  @Override
  public View getView(int position, View row, ViewGroup parent) {

    // load any blocks for the current position
    loadBlocks(position);

    // view holder pattern
    ViewHolder viewHolder = null;
    if (row == null) {

      row = inflater.inflate(R.layout.singly_friends_row, parent, false);
      TextView sectionHeader = (TextView)row
        .findViewById(R.id.singlyFriendsRowSectionHeader);
      TextView friendNameView = (TextView)row
        .findViewById(R.id.singlyFriendsRowName);
      ProgressBar friendProgressView = (ProgressBar)row
        .findViewById(R.id.singlyFriendsRowProgress);
      ImageView friendImageView = (ImageView)row
        .findViewById(R.id.singlyFriendsRowImage);

      viewHolder = new ViewHolder();
      viewHolder.sectionHeader = sectionHeader;
      viewHolder.name = friendNameView;
      viewHolder.progress = friendProgressView;
      viewHolder.image = friendImageView;

      row.setTag(viewHolder);
    }
    else {
      viewHolder = (ViewHolder)row.getTag();
    }

    // set the section header if we have one
    viewHolder.sectionHeader.setVisibility(View.GONE);
    if (sectionPositions != null && sectionPositions.containsKey(position)) {
      viewHolder.sectionHeader.setVisibility(View.VISIBLE);
      viewHolder.sectionHeader.setText(sectionPositions.get(position));
    }

    // display the row or loading if the row isn't available yet
    viewHolder.name.setText("");
    viewHolder.image.setImageBitmap(null);
    Friend friend;

    // get the row object
    friend = getBackingObject(position);
    if (friend != null) {

      String friendName = friend.name;
      if (StringUtils.isNotBlank(friendName)) {
        viewHolder.progress.setVisibility(View.GONE);
        viewHolder.name.setText(friendName);
      }

      if (displayImages && remoteImageCache != null) {

        // populate the default image if needed
        if (defaultImage == null) {
          defaultImage = BitmapFactory.decodeResource(context.getResources(),
            defaultImageResource);
        }

        // make the image view visible
        viewHolder.image.setVisibility(View.VISIBLE);

        // setup the image to get or download, we want a 32x32 image
        ImageInfo imageInfo = new ImageInfo();
        String id = StringUtils.replace(friend.name, " ", "_");
        id = StringUtils.lowerCase(id);
        imageInfo.id = id;
        imageInfo.imageUrl = friend.imageUrl;
        imageInfo.width = 42;
        imageInfo.height = 42;
        imageInfo.format = Bitmap.CompressFormat.JPEG;
        imageInfo.quality = 80;
        imageInfo.sample = true;

        // setup a listener to just change the one image view instead of
        // calling
        // notifyDataSetChanged which redraws the entire list view screen
        final ListView mainListView = (ListView)parent;
        final int curPosition = position;

        // optimization to only change the single image in the single row
        // should the row still be visible when the image is done downloading
        // in the background
        imageInfo.listener = new ImageCacheListener() {

          @Override
          public void onSuccess(ImageInfo imageInfo, Bitmap bitmap) {

            // get the start and end visible rows in the list view
            int startRow = mainListView.getFirstVisiblePosition();
            int endRow = mainListView.getLastVisiblePosition();

            // if current position is visible get the specific row and change
            // just the image in that row. This will cause the image to update
            // on the fly even if no scrolling is happening. Nothing else in
            // the row will be invalidated
            if (curPosition >= startRow && curPosition <= endRow) {
              View rowView = mainListView.getChildAt(curPosition - startRow);
              ImageView imageView = (ImageView)rowView
                .findViewById(R.id.singlyFriendsRowImage);
              imageView.setImageBitmap(bitmap);
            }
          }

        };

        // get the friend image or the default
        Bitmap friendImage = remoteImageCache.getImage(imageInfo);
        if (friendImage == null) {
          friendImage = defaultImage;
        }
        viewHolder.image.setImageBitmap(friendImage);
      }
    }
    else {

      // loading friends, show loading text
      viewHolder.name.setText("Loading...");
      viewHolder.progress.setVisibility(View.VISIBLE);
    }

    return row;
  }

  public boolean isDisplaySectionHeaders() {
    return displaySectionHeaders;
  }

  public void setDisplaySectionHeaders(boolean displaySectionHeaders) {
    this.displaySectionHeaders = displaySectionHeaders;
  }

  public boolean isDisplayImages() {
    return displayImages;
  }

  public void setDisplayImages(boolean displayImages) {
    this.displayImages = displayImages;
  }

  public int getDefaultImageResource() {
    return defaultImageResource;
  }

  public void setDefaultImageResource(int defaultImageResource) {
    this.defaultImageResource = defaultImageResource;
  }

  public RemoteImageCache getRemoteImageCache() {
    return remoteImageCache;
  }

  public void setRemoteImageCache(RemoteImageCache remoteImageCache) {
    this.remoteImageCache = remoteImageCache;
  }

}
