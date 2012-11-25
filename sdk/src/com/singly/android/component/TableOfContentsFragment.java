package com.singly.android.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.singly.android.client.AsyncApiResponseHandler;
import com.singly.android.client.SinglyClient;
import com.singly.android.client.SinglyClient.Authentication;
import com.singly.android.sdk.R;
import com.singly.android.util.JSON;

/**
 * A Fragment component that displays a table of contents for a list of friends.  
 * This Fragment wraps the Singly /friends API.
 * 
 * To use the TableOfContentsFragment you will want to add it to an Activity. 
 * The parent activity can implement {@link TableOfContentsTouchListener} to 
 * handle when the TableOfContentsFragment is touched.
 */
public class TableOfContentsFragment
  extends Fragment {

  protected Activity activity;
  protected LinearLayout tableOfContentsWrapperLayout;
  protected LinearLayout tableOfContentsLayout;

  protected Map<String, Integer> tableOfContents;
  protected SinglyClient singlyClient;
  protected String accessToken;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    this.activity = activity;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {

    super.onCreateView(inflater, container, savedInstanceState);

    // get the singly client and access token
    this.singlyClient = SinglyClient.getInstance();
    Authentication auth = singlyClient.getAuthentication(activity);
    this.accessToken = auth.accessToken;

    // create the table of contents layout
    tableOfContentsWrapperLayout = (LinearLayout)inflater.inflate(
      R.layout.singly_toc_fragment, container, false);
    tableOfContentsLayout = (LinearLayout)tableOfContentsWrapperLayout
      .findViewById(R.id.singlyTableOfContentsLayout);

    // get the access token and query parameters
    Map<String, String> qparams = new HashMap<String, String>();
    qparams.put("access_token", accessToken);
    qparams.put("offset", "0");
    qparams.put("limit", "1");
    qparams.put("toc", "true");

    // make a call to the api to get the table of contents
    singlyClient.doGetApiRequest(activity, "/friends/all", qparams,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {

          JsonNode root = JSON.parse(response);
          for (JsonNode node : root) {

            if (tableOfContents == null) {

              tableOfContents = new HashMap<String, Integer>();
              Map<String, JsonNode> tocFields = JSON.getFields(node);
              for (Map.Entry<String, JsonNode> tocField : tocFields.entrySet()) {
                String tocKey = tocField.getKey();
                JsonNode tocNode = tocField.getValue();
                if (!StringUtils.equals(tocKey, "meta")) {
                  tableOfContents.put(StringUtils.upperCase(tocKey),
                    JSON.getInt(tocNode, "offset"));
                }
              }

              // only put in letters that have entries, everything else is a .
              // and we condense multiple . into one. The everything else *
              // doesn't show as a . it either exists or it doesn't
              char[] letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ*".toCharArray();
              final List<String> tocEntries = new ArrayList<String>();
              for (int i = 0; i < letters.length; i++) {
                int numEntries = tocEntries.size();
                String letter = String.valueOf(letters[i]);
                if (tableOfContents.containsKey(letter)) {
                  tocEntries.add(letter);
                }
                else if (numEntries > 0
                  && !tocEntries.get(numEntries - 1).equals(".")
                  && !letter.equals("*")) {
                  tocEntries.add(".");
                }
              }

              // create the table of contents
              final int tocHeight = tableOfContentsLayout.getHeight();
              final int tocLength = tocEntries.size();
              final double pixelsPerItem = (double)tocHeight / tocLength;
              int fontSize = (int)(pixelsPerItem * 0.80d);

              // setup the keys for the table of contents
              for (String key : tocEntries) {

                TextView letterTextView = new TextView(activity);
                letterTextView.setText(String.valueOf(key));
                letterTextView.setGravity(Gravity.CENTER
                  | Gravity.CENTER_VERTICAL);
                letterTextView.setPadding(10, 0, 10, 0);
                letterTextView
                  .setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
                LayoutParams params = new LayoutParams(
                  LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
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

                  // get the letter in the toc in the index and from that the
                  // position that starts that letter, pass that into the
                  // listener
                  String letter = tocEntries.get(tocIndex);
                  int pos = tableOfContents.get(tocEntries.get(tocIndex));
                  if (!letter.equals(".") && pos >= 0 && activity != null
                    && activity instanceof TableOfContentsTouchListener) {
                    ((TableOfContentsTouchListener)activity)
                      .onTableOfContentsTouched(letter, pos);
                  }

                  return true;
                }
              });

              break;
            }
          }
        }
      });

    return tableOfContentsWrapperLayout;
  }

  public Map<String, Integer> getTableOfContents() {
    return Collections.unmodifiableMap(tableOfContents);
  }

}
