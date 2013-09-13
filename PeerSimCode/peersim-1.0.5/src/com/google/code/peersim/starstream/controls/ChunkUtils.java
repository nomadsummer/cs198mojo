package com.google.code.peersim.starstream.controls;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.pastry.protocol.PastryResource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import peersim.core.CommonState;

/**
 * This class is useful to disparate other components that need to deal with chunks.
 * It must be used as a <i>chunks-factory</i> (i.e. by the {@link StarStreamSource}),
 * and as a centralized <i>generated chunk identifiers repository</i> by *-Stream
 * Chunk Schedulers.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkUtils {

  /**
   * Memory of generated chunk-identifiers. It has a two-level depth, the first one
   * being the *-Stream session identifier (an UUID), and the second being a chunk's
   * sequence identifier. The final value that can be accessed is the concrete chunk
   * identifier, that is a {@link PastryId}.
   */
  private static Map<UUID, Map<Integer, PastryId>> chunkIds = new HashMap<UUID, Map<Integer, PastryId>>();

  private static int minSeqNumber = -1;

  public static PastryId getChunkIdForSequenceId(UUID sessionId, int seqId) {
    PastryId res = null;
    Map<Integer, PastryId> chunks = chunkIds.get(sessionId);
    if(chunks!=null) {
      res = chunks.get(seqId);
    }
    return res;
  }

  public static List<PastryId> getChunkIdsForSequenceIds(UUID sessionId, List<Integer> seqIds) {
    List<PastryId> pids = new ArrayList<PastryId>();
    Map<Integer, PastryId> chunks = chunkIds.get(sessionId);
    if(chunks!=null) {
      for(int id : seqIds) {
        PastryId pid = chunks.get(id);
        if(pid!=null)
          pids.add(pid);
      }
    }
    return pids;
  }

  /**
   * Factory method.
   * 
   * @param <T> The actual payload type
   * @param data The chunk payload
   * @param sid The *-Stream unique session ID
   * @param seqNumber The chunk sequence number
   * @return The new chunk
   */
  static <T> Chunk<T> createChunk(T data, UUID sid, int seqNumber, int ttl) {
    // TODO multisession minSeqNum
    minSeqNumber = Math.min(seqNumber, minSeqNumber);
    Chunk<T> chunk = new Chunk<T>(data, sid, seqNumber, ttl);
    storeNewChunkIdentity(chunk);
    return chunk;
  }

  public static int getMinSeqNumber() {
    return minSeqNumber;
  }

  /**
   * Returns the unique identifier of the produced chunk belonging to session
   * {@code sid} and with seuqence number equal to {@code seqNumber+1}, if it
   * exists, {@code null} otherwise.
   *
   * @param sid The *-Stream session identifier
   * @param seqNumber The last seen sequence number
   * @return The very next chunk unique ID, or {@code null}
   */
  public static PastryId nextChunkId(UUID sid, int seqNumber) {
    PastryId res = null;
    Map<Integer, PastryId> ids = chunkIds.get(sid);
    if (ids != null) {
      res = ids.get(seqNumber + 1);
    } else {
      // no entry for the give sid
      // NOP
    }
    return res;
  }

  /**
   * Return the sequence number for the latest generate chunk for the required
   * streaming session if there is one. In anyother case {@link Integer#MIN_VALUE}
   * is returned.
   *
   * @param sessionID
   * @return
   */
  static int getLastGeneratedChunkSeqId(UUID sessionID) {
    Integer max = Integer.MIN_VALUE;
    Map<Integer, PastryId> chunksForSession = chunkIds.get(sessionID);
    if(chunksForSession!=null && !chunksForSession.isEmpty()) {
      Set<Integer> seqIds = chunksForSession.keySet();
      for(int seqId : seqIds) {
        if(seqId > max) {
          max = seqId;
        }
      }
    }
    return max;
  }

  /**
   * Stores the {@link PastryId} associated with the given chunk in the internal
   * memory.
   *
   * @param chunk The new chunk whose identity has to be memorized
   */
  private static void storeNewChunkIdentity(Chunk<?> chunk) {
    Map<Integer, PastryId> ids = chunkIds.get(chunk.getSessionId());
    if (ids == null) {
      ids = new HashMap<Integer, PastryId>();
      chunkIds.put(chunk.getSessionId(), ids);
    }
    ids.put(chunk.getSequenceId(), chunk.getResourceId());
  }

  /**
   * This class is used to represent chunks that must be disseminated to every node
   * during the streaming event. Being a {@link PastryResource}, it is uniquely
   * identified by a {@link PastryId} instance.
   *
   * @author frusso
   * @version 0.1
   * @since 0.1
   */
  public static class Chunk<T> extends PastryResource<T> implements Comparable<Chunk<?>> {

    private final UUID sessionId;
    private final int sequenceId;
    private final long timeStamp;
    private final int ttl;

    /**
     * Constructor.
     * @param chunk The actual content
     * @param sid The streaming-session identifier
     * @param seq The sequence number
     */
    private Chunk(T chunk, UUID sid, int seq, int ttl) {
      super(chunk);
      sessionId = sid;
      sequenceId = seq;
      timeStamp = CommonState.getTime();
      this.ttl = ttl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Chunk<?> other) {
      int res = 0;
      // if this and other actually differ...
      if(!this.equals(other)) {
        // if their session ids equal...
        if(sessionId.equals(other.sessionId)) {
          // leverage the sequence number
          res = sequenceId-other.sequenceId;
        } else {
          // otherwise simply compare the two session ids
          res = sessionId.compareTo(other.sessionId);
        }
      }
      return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
      if(!(other instanceof Chunk))
        return false;
      Chunk<?> otherChunk = (Chunk<?>) other;
      return super.equals(other) && sequenceId==otherChunk.sequenceId && sessionId.equals(otherChunk.sessionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      int hash = 5;
      hash = 89 * hash + (this.sessionId != null ? this.sessionId.hashCode() : 0);
      hash = 89 * hash + this.sequenceId;
      hash = 89 * hash + super.hashCode();
      return hash;
    }

    /**
     * Returns the sequence number associated with this chunk of data.
     * @return The sequence number
     */
    public int getSequenceId() {
      return sequenceId;
    }

    /**
     * Returns the session id associated with this chunk of data.
     * @return The session identifier
     */
    public UUID getSessionId() {
      return sessionId;
    }

    public long getTimeStamp() {
      return timeStamp;
    }

    public int getTTL() {
      return ttl;
    }

    public boolean isExpired() {
      return CommonState.getTime() > timeStamp + ttl;
    }

    @Override
    public String toString() {
      return super.toString()+" TTL "+getTTL()+" Timestamp "+getTimeStamp();
    }
  }
}