package com.singly.android.component;

/**
 * A Listener interface for when a row in the FriendsListFragment is clicked.
 */
public interface FriendsListRowClickListener {

  /**
   * Called when a row in the FriendsListFragment is clicked.  The Friend object
   * representing that row, along with the row position is passed.
   * 
   * @param friend The Friend object represented by the row clicked.
   * @param pos The position in the list that was clicked.
   */
  public void onFriendClicked(Friend friend, int pos);

}
