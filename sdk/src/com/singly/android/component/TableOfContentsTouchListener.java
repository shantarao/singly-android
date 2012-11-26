package com.singly.android.component;

/**
 * A Listener interface for when a view in the TableOfContentsFragment is 
 * clicked.  
 * 
 * Usually when a view or letter in the table of contents is clicked then the
 * FriendsListFragment is scrolled to the position in its list where names with
 * that letter start.
 */
public interface TableOfContentsTouchListener {

  /**
   * Called when a view in the TableOfContentsFragment is clicked.
   * 
   * @param letter The letter in the table of contents that was clicked.
   * @param position The position that the FriendsListFragment should be 
   * scrolled to where names starting with the letter begin.
   */
  public void onTableOfContentsTouched(String letter, int position);

}
