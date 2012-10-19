package com.singly.android.component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.view.View;
import android.view.ViewGroup;

/**
 * An AbstractCachingBlockLoadedListAdapter implementation that also supports
 * tables of content.
 * 
 * @param <T> The type of the rows loaded in the adapter.
 */
public abstract class TableOfContentsListAdapter<T>
  extends AbstractCachingBlockLoadedListAdapter<T> {

  protected Map<String, Integer> tableOfContents;
  protected Map<Integer, String> tablePositions;

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
  public TableOfContentsListAdapter(int rows, int blockSize,
    int blocksToPreload, int blocksToCache) {
    super(rows, blockSize, blocksToPreload, blocksToCache);
  }

  /**
   * Sets up table of contents labels and positions.
   * 
   * @param tableOfContents The table of contents labels and label position.
   * The positions are the row positions where the labels start.
   */
  protected void initializeTableOfContents(Map<String, Integer> tableOfContents) {

    // setup the table of contents and section positions
    this.tableOfContents = tableOfContents;
    if (tableOfContents != null && tableOfContents.size() > 0) {
      this.tablePositions = Collections
        .synchronizedMap(new HashMap<Integer, String>());
      for (Map.Entry<String, Integer> entry : tableOfContents.entrySet()) {
        tablePositions.put(entry.getValue(), entry.getKey());
      }
    }

    notifyDataSetChanged();
  }

  @Override
  protected abstract void loadBlock(int blockId, int offset, int limit);

  @Override
  public abstract View getView(int position, View row, ViewGroup parent);

  public Map<String, Integer> getTableOfContents() {
    return Collections.unmodifiableMap(tableOfContents);
  }

  public Map<Integer, String> getTablePositions() {
    return Collections.unmodifiableMap(tablePositions);
  }

  public int getTableOfContentsPosition(String tocEntry) {
    return tableOfContents == null ? -1 : tableOfContents.get(tocEntry);
  }

  public boolean isTableOfContentsHeader(int position) {
    return tablePositions != null && tablePositions.containsKey(position);
  }

  public String getTableOfContentsEntry(int position) {
    return tablePositions == null ? null : tablePositions.get(position);
  }
}
