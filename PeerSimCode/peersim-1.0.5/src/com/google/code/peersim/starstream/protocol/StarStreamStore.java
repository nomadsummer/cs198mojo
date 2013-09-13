/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.controls.ChunkUtils.Chunk;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Instances of this class are used by {@link StarStreamProtocol} instances to
 * store chunks exchanged with other nodes that must be made available to any
 * higher layer, for playback i.e.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamStore {

  /**
   * Internal representation of the store.
   */
  private Map<UUID, Map<PastryId, Chunk<?>>> store;
  // TODO multi-session
  private SortedSet<Chunk<?>> orderedStore;
  private Set<Integer> storeHistory;
  private Set<Integer> rejectedChunksDueToExpiration;
  private Set<Integer> rejectedChunksDueToCapacityLimit;
  private int maxSize;

  public Set<Integer> getRejectedChunksDueToCapacityLimit() {
    return Collections.unmodifiableSet(rejectedChunksDueToCapacityLimit);
  }

  public Set<Integer> getRejectedChunksDueToExpiration() {
    return Collections.unmodifiableSet(rejectedChunksDueToExpiration);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<UUID, Map<PastryId, Chunk<?>>> entry : store.entrySet()) {
      UUID sid = entry.getKey();
      purge(sid);
      sb.append("SessionId: " + sid + "\n");
      Map<PastryId, Chunk<?>> chunks = entry.getValue();
      sb.append("Size: " + chunks.size() + "\n");
      int i = 0;
      for (Map.Entry<PastryId, Chunk<?>> chunk : chunks.entrySet()) {
        sb.append((i++) + ") " + chunk.getValue() + "\n");
      }
    }
    return sb.toString();
  }

  /**
   * Constructor.
   */
  StarStreamStore(int maxSize) {
    store = new HashMap<UUID, Map<PastryId, Chunk<?>>>();
    orderedStore = new TreeSet<Chunk<?>>();
    rejectedChunksDueToExpiration = new HashSet<Integer>();
    rejectedChunksDueToCapacityLimit = new HashSet<Integer>();
    storeHistory = new HashSet<Integer>();
    this.maxSize = maxSize;
  }

  /**
   * Stores the given chunk iff it not already in the store.
   * @param chunk The chunk to be added
   * @return Whether the chunk has been added or not
   */
  boolean addChunk(Chunk<?> chunk) {
    boolean added = false;
    Map<PastryId, Chunk<?>> chunks = store.get(chunk.getSessionId());
    if(chunks!=null) {
      if(!chunks.containsKey(chunk.getResourceId())) {
        // the chunk has to be stored iff it has not expired yet
        if(!chunk.isExpired()) {
          // the chunk is valid and can be added as long as the max-size is not
          // exceeded
          if(size()<maxSize) {
            added = true;
          } else {
            // size limit!
            rejectedChunksDueToCapacityLimit.add(chunk.getSequenceId());
          }
        } else {
          // the chunk has already expired
          if(!storeHistory.contains(chunk.getSequenceId()))
            rejectedChunksDueToExpiration.add(chunk.getSequenceId());
        }
      } else {
        // the chunk is already in
        // NOP
      }
    } else {
      // the first time we see that session id: the chunk can be immediately stored
      chunks = new HashMap<PastryId, Chunk<?>>();
      store.put(chunk.getSessionId(), chunks);
      added = true;
    }
    if(added) {
      added = true;
      chunks.put(chunk.getResourceId(), chunk);
      orderedStore.add(chunk);
      storeHistory.add(chunk.getSequenceId());
    }
    return added;
  }

  /**
   * Returns the chunk stored with the specified identifiers, or {@code null}
   * if it is not available.
   *
   * @param sessionId The *-Stream session id
   * @param chunkId The *-Stream chunk id
   * @return The requested chunk or {@code null} if it is not available
   */
  Chunk<?> getChunk(UUID sessionId, PastryId chunkId) {
    Chunk<?> chunk = null;
    Map<PastryId, Chunk<?>> chunks = store.get(sessionId);
    if (chunks != null) {
      chunk = chunks.get(chunkId);
      // remove and return null if expired
      if (chunk != null && chunk.isExpired()) {
        // remove from map...
        chunks.remove(chunkId);
        // remove from set...
        orderedStore.remove(chunk);
        // nullify return value
        chunk = null;
      }
    }
    return chunk;
  }

  /**
   * Tells whether the chunk uniquely identified by the provided *-Stream session
   * identifier and *-Stream chunk identifier (a {@link PastryId} actually) is
   * already in the store or not.
   *
   * @param sessionId The *-Stream session id
   * @param chunkId The *-Stream chunk id
   * @return Whether the chunk is stored or not
   */
  boolean isStored(UUID sessionId, PastryId chunkId) {
    return getChunk(sessionId, chunkId) != null;
  }

  int countContiguousChunksFromStart(UUID sessionId) {
    int count = 1;
    purge(sessionId);
    Chunk<?>[] chunks = orderedStore.toArray(new Chunk<?>[orderedStore.size()]);
    for(int i=1; i<chunks.length; i++) {
      int right = chunks[i].getSequenceId();
      int left = chunks[i-1].getSequenceId();
      if(right-left==1) {
        count++;
      } else {
        break;
      }
    }
    return count;
  }

  List<Integer> getMissingSequenceIds(UUID sessionId) {
    List<Integer> ids = new ArrayList<Integer>();
    purge(sessionId);
    Chunk<?>[] chunks = orderedStore.toArray(new Chunk<?>[orderedStore.size()]);
    for(int i=1; i<chunks.length; i++) {
      int right = chunks[i].getSequenceId();
      int left = chunks[i-1].getSequenceId();
      for(int j=left+1; j<right; j++) {
        ids.add(j);
      }
    }
    return ids;
  }

  private void purge(UUID sessionId) {
    List<PastryId> expired = new ArrayList<PastryId>();
    Map<PastryId, Chunk<?>> chunks = store.get(sessionId);
    if (chunks != null) {
      for (Map.Entry<PastryId, Chunk<?>> chunk : chunks.entrySet()) {
        if (chunk.getValue().isExpired()) {
          expired.add(chunk.getKey());
        }
      }
      for (PastryId id : expired) {
        // remove from map...
        Chunk<?> removed = chunks.remove(id);
        // remove from set...
        orderedStore.remove(removed);
      }
    }
  }

  /**
   * Tells how many chunks are stored.
   *
   * @return The number of stored chunks
   */
  private int size() {
    int size = 0;
    for (Map.Entry<UUID, Map<PastryId, Chunk<?>>> entry : store.entrySet()) {
      purge(entry.getKey());
      size += entry.getValue().size();
    }
    return size;
  }
}