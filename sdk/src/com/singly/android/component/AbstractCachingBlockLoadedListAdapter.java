package com.singly.android.component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * BaseAdapter class that dynamically loads and caches rows in blocks.  This
 * allows displaying lists of any size efficiently.
 * 
 * Subclasses must adhere to following contract.
 * 
 * <ol>
 * <li>Implement the {@link #getView(int, View, ViewGroup)} method to setup 
 * both a ViewHolder pattern and the actual row display.</li>
 * <li>The getView method must call {@link #loadBlocks(int)} to load any blocks
 * required for that position.</li>
 * <li>Implement the {@link #loadBlock(int, int, int)} method to load a single 
 * block in their preferred method.  Usually this is a call to a web service 
 * and the block rows are parsed and returned.</li>
 * <li>Once a block has been retrieved within the loadBlock method, then the 
 * {@link #finishAndCacheBlock(int, List)} method <b>must</b> be called with
 * the new block passed as input.  This will handle synchronization and caching 
 * of the block to ensure a block is only loaded once.</li>
 * </ol>
 * 
 * The backing row object is not defined, nor is the layout for the row.  These
 * are decision left up to the subclass and handled in getView.
 *
 * @param <T> The object type of the backing row.
 */
public abstract class AbstractCachingBlockLoadedListAdapter<T>
  extends BaseAdapter {

  // block handling
  private int maxBlockId = 0;
  private int lastCheckpoint = 0;
  private int rows = 0;
  private int blocks = 0;
  private int blockSize = 20;
  private int blocksToPreload = 2;
  private int blocksToCache = 50;

  private LruCache<Integer, List<T>> blockCache;
  private Set<Integer> blocksLoading = Collections
    .synchronizedSet(new HashSet<Integer>());
  private Set<Integer> blocksLoaded = Collections
    .synchronizedSet(new HashSet<Integer>());

  /**
   * Attempts to load a number of blocks into the cache.  This would be the 
   * current block for the position and the blocksToPreload on either side of
   * the current block.
   * 
   * If blocks are already loaded into the cache they are not reloaded.  There
   * are also optimizations for moving halfway through a block before attempting 
   * to load other blocks.
   * 
   * @param position The position in the list to load a block for.
   */
  protected void loadBlocks(int position) {

    // position in block and current block id
    int boundedPos = Math.min(Math.max(position, 0), rows - 1);
    int posBlockId = boundedPos / blockSize;
    int checkDelta = Math.abs(boundedPos - lastCheckpoint);

    // optimization, only try to load new blocks if we went half block size
    // we only reset the checkpoint when we try and load blocks
    if (lastCheckpoint == 0 || checkDelta >= blockSize / 2) {

      lastCheckpoint = boundedPos;
      int[] blocksToLoad = new int[(blocksToPreload * 2) + 1];
      blocksToLoad[0] = posBlockId;

      // previous block ids
      for (int i = blocksToPreload; i > 0; i--) {
        int curIndex = (blocksToPreload - i) + 1;
        blocksToLoad[curIndex] = -1;
        int prevBlockId = posBlockId - curIndex;
        if (prevBlockId >= 0) {
          blocksToLoad[curIndex] = prevBlockId;
        }
      }

      // next block ids
      for (int i = 0; i < blocksToPreload; i++) {
        int curIndex = (blocksToPreload + i) + 1;
        blocksToLoad[curIndex] = -1;
        int nextBlockId = posBlockId + (i + 1);
        if (nextBlockId < maxBlockId) {
          blocksToLoad[curIndex] = nextBlockId;
        }
      }

      // load any new blocks
      for (int i = 0; i < blocksToLoad.length; i++) {

        // get the current block id, ignore bounded ids
        int curBlockId = blocksToLoad[i];
        if (curBlockId < 0) {
          continue;
        }

        // if not loading then prepare for loading in a guarded manner
        boolean loadBlock = false;
        synchronized (this) {
          if (blockCache.get(curBlockId) == null
            && !blocksLoading.contains(curBlockId)) {
            blocksLoading.add(curBlockId);
            loadBlock = true;
          }
        }

        // if we should load this block, meaning not loaded and not currently in
        // a loading state
        if (loadBlock) {
          int offset = (curBlockId * blockSize);
          int limit = blockSize;
          loadBlock(curBlockId, offset, limit);
        }
      }
    }
  }

  /**
   * Load a single block or rows from the offset and limit.  Subclasses will
   * need to call {@link #finishAndCacheBlock(int, List)} at the end of this
   * method to complete the loading and caching process.
   * 
   * @param blockId The block id to load.
   * @param offset The offset of rows to load.
   * @param limit The number of rows to load.
   */
  protected abstract void loadBlock(int blockId, int offset, int limit);

  /**
   * Completes the loading and caching process.  This method must be called by
   * subclasses implementing the {@link #loadBlock(int, int, int)} method.
   * 
   * @param blockId The block id to finish.
   * @param block The block of rows represented by the block id.
   */
  protected void finishAndCacheBlock(int blockId, List<T> block) {

    // guard adding to cache and removing from loading state
    synchronized (this) {
      blockCache.put(blockId, block);
      blocksLoaded.add(blockId);
      blocksLoading.remove(blockId);
    }

    // update any visible rows that might be waiting
    notifyDataSetChanged();
  }

  /**
   * Returns the block id and block position for the current position.
   * 
   * The block id is the block that contains the row at position.  The block
   * position is the position in the block that represents that row.
   * 
   * @param position The row position in the list.
   * 
   * @return An array containing two entries, block id and block position.
   */
  protected int[] getBlockIdAndPosition(int position) {

    // get the block id and block position from the row position
    int boundedPosition = Math.min(Math.max(position, 0), rows - 1);
    int blockId = boundedPosition / blockSize;
    int blockPos = boundedPosition % blockSize;

    return new int[] {
      blockId, blockPos
    };
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
  public AbstractCachingBlockLoadedListAdapter(int rows, int blockSize,
    int blocksToPreload, int blocksToCache) {

    // block handling configuration
    this.rows = rows;
    this.blockSize = blockSize;
    this.blocksToPreload = blocksToPreload;
    this.blocksToCache = blocksToCache;

    // precompute the number of total blocks
    this.blocks = rows / blockSize;
    if (rows % blockSize > 0) {
      blocks += 1;
    }

    // set the max block id used to bound when loading blocks, the min block
    // id is always 0
    if (blocks > 0) {
      this.maxBlockId = blocks - 1;
    }

    // block cache is Lru cache based on gets and puts
    blockCache = new LruCache<Integer, List<T>>(blocksToCache);
  }

  /**
   * Returns the backing object, aka row, for the position.  The backing object
   * is the object that was cached for a given position.  Blocks contains one
   * or more backing objects.
   * 
   * @param position The row position in the list.
   * 
   * @return The backing object at the position.
   */
  public T getBackingObject(int position) {

    // get the block id and block position from the row position
    int[] blockIdAndPos = getBlockIdAndPosition(position);
    int blockId = blockIdAndPos[0];
    int blockPos = blockIdAndPos[1];

    // block is loaded
    if (blocksLoaded.contains(blockId)) {

      // row is good, return from the block
      List<T> block = blockCache.get(blockId);
      if (block != null && block.size() > 0 && block.size() > blockPos) {
        return block.get(blockPos);
      }
    }

    // block still loading or not good
    return null;
  }

  @Override
  public abstract View getView(int position, View row, ViewGroup parent);

  @Override
  public int getCount() {
    return rows;
  }

  @Override
  public String getItem(int position) {
    return String.valueOf(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  public int getBlocksToPreload() {
    return blocksToPreload;
  }

  public void setBlocksToPreload(int blocksToPreload) {
    this.blocksToPreload = blocksToPreload;
  }

  public int getBlockSize() {
    return blockSize;
  }

  public int getBlocksToCache() {
    return blocksToCache;
  }

}
