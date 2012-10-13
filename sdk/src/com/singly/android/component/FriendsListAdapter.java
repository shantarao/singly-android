package com.singly.android.component;

import java.util.ArrayList;
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
import com.singly.android.sdk.R;
import com.singly.android.util.ImageCacheListener;
import com.singly.android.util.ImageInfo;
import com.singly.android.util.JSON;
import com.singly.android.util.RemoteImageCache;
import com.singly.android.util.SinglyUtils;

/**
 * A {@link TableOfContentsListAdapter} implementation customized for use with
 * the Singly Friends API.
 */
public class FriendsListAdapter
  extends TableOfContentsListAdapter<Friend> {

  private LayoutInflater inflater;
  private Context context;
  private SinglyClient singlyClient = SinglyClient.getInstance();
  private RemoteImageCache imageCache;
  private Bitmap defaultImage;

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
    qparams.put("access_token", SinglyUtils.getAccessToken(context));
    qparams.put("offset", String.valueOf(offset));
    qparams.put("limit", String.valueOf(limit));
    qparams.put("toc", "true");

    // make a call to the api to get the block
    singlyClient.doGetApiRequest(context, "/friends/all", qparams,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {

          List<Friend> blockOfFriends = new ArrayList<Friend>();
          boolean isFirstNode = true;
          JsonNode root = JSON.parse(response);

          for (JsonNode node : root) {

            // parse and initialize the table of contents from first row
            if (tableOfContents == null && tablePositions == null
              && isFirstNode) {

              Map<String, Integer> tableOfContents = new HashMap<String, Integer>();
              Map<String, JsonNode> tocFields = JSON.getFields(node);
              for (Map.Entry<String, JsonNode> tocField : tocFields.entrySet()) {
                String tocKey = tocField.getKey();
                JsonNode tocNode = tocField.getValue();
                if (!StringUtils.equals(tocKey, "meta")) {
                  tableOfContents.put(StringUtils.upperCase(tocKey),
                    JSON.getInt(tocNode, "offset"));
                }
              }

              initializeTableOfContents(tableOfContents);
            }

            // beyond first node is friends, first node is toc
            if (!isFirstNode) {

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

            // passed first node
            isFirstNode = false;
          }

          // cache the block
          finishAndCacheBlock(blockId, blockOfFriends);
        }

        @Override
        public void onFailure(Throwable error) {
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
   * @param imageCache The remote image cache used to download, store, and 
   * cache friend images.  If null images will not be displayed.
   */
  public FriendsListAdapter(Context context, int rows, int blockSize,
    int blocksToPreload, int blocksToCache, RemoteImageCache imageCache) {

    super(rows, blockSize, blocksToPreload, blocksToCache);
    this.context = context;
    this.inflater = (LayoutInflater)context
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    // image handling
    if (imageCache != null) {
      this.imageCache = imageCache;
      this.defaultImage = BitmapFactory.decodeResource(context.getResources(),
        R.drawable.friend_noimage);
    }

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
        .findViewById(R.id.tableOfContentsSectionHeader);
      TextView friendNameView = (TextView)row.findViewById(R.id.friendName);
      ProgressBar friendProgressView = (ProgressBar)row
        .findViewById(R.id.friendProgress);
      ImageView friendImageView = (ImageView)row.findViewById(R.id.friendImage);

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

    // set the section header if we have a toc and it is a toc section start
    viewHolder.sectionHeader.setVisibility(View.GONE);
    if (isTableOfContentsHeader(position)) {
      viewHolder.sectionHeader.setVisibility(View.VISIBLE);
      viewHolder.sectionHeader.setText(getTableOfContentsEntry(position));
    }

    // display the row or loading if the row isn't available yet
    viewHolder.name.setText("");
    viewHolder.image.setImageBitmap(null);
    final Friend friend = getBackingObject(position);
    if (friend != null) {

      String friendName = friend.name;
      if (StringUtils.isNotBlank(friendName)) {
        viewHolder.progress.setVisibility(View.GONE);
        viewHolder.name.setText(friendName);
      }

      if (imageCache != null) {

        // make the image view visible
        viewHolder.image.setVisibility(View.VISIBLE);

        // setup the image to get or download, we want a 32x32 image
        ImageInfo imageInfo = new ImageInfo();
        String id = StringUtils.replace(friend.name, " ", "_");
        id = StringUtils.lowerCase(id);
        imageInfo.id = id;
        imageInfo.imageUrl = friend.imageUrl;
        imageInfo.width = 32;
        imageInfo.height = 32;
        imageInfo.format = Bitmap.CompressFormat.JPEG;
        imageInfo.quality = 80;
        imageInfo.sample = true;

        // setup a listener to just change the one image view instead of calling
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
                .findViewById(R.id.friendImage);
              imageView.setImageBitmap(bitmap);
            }
          }

        };

        // get the friend image or the default
        Bitmap friendImage = imageCache.getImage(imageInfo);
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

}
