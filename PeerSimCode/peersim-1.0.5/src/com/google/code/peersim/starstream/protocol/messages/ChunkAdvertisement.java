package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.controls.ChunkUtils.Chunk;
import com.google.code.peersim.starstream.protocol.StarStreamNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This message is used to advertise the availability of a new chunk at the
 * (at least) advertising node. This message simply carries the chunk unique
 * identifier. Interested nodes have to reply this message with the appropriate
 * {@link ChunkRequest} to obtain the advertised chunk.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkAdvertisement extends StarStreamMessage {

  /**
   * Factory method for creating a list of {@link ChunkAdvertisement}s that must be
   * sent by {@code src} to {@code dsts}.
   *
   * @param src The source
   * @param dsts The destinations
   * @param chunk The chunk
   * @return The messages
   */
  public static List<ChunkAdvertisement> newInstancesFor(StarStreamNode src, Set<StarStreamNode> dsts, Chunk<?> chunk) {
    UUID sessionId = chunk.getSessionId();
    PastryId chunkId = chunk.getResourceId();
    List<ChunkAdvertisement> res = new ArrayList<ChunkAdvertisement>();
    for(StarStreamNode dst : dsts) {
      res.add(new ChunkAdvertisement(src, dst, sessionId, chunkId));
    }
    return res;
  }

  /**
   * The advertised chunk's unique identifier.
   */
  private final PastryId chunkId;
  /**
   * The advertised chunk's session identifier.
   */
  private final UUID sessionId;

  /**
   * Constructor. When creating a new instance, the specified source is also used to
   * initialize the message originator.
   *
   * @param src The sender
   * @param dst The destination
   * @param chunk The chunk
   */
  ChunkAdvertisement(StarStreamNode src, StarStreamNode dst, UUID sessionId, PastryId chunkId) {
    super(src, dst);
    if(src==null) throw new IllegalArgumentException("The source cannot be 'null'");
    if(dst==null) throw new IllegalArgumentException("The destination cannot be 'null'");
    if(chunkId==null) throw new IllegalArgumentException("The chunk identifier cannot be 'null'");
    if(sessionId==null) throw new IllegalArgumentException("The chunk session id cannot be 'null'");
    this.chunkId = chunkId;
    this.sessionId = sessionId;
  }

  /**
   * Returns a reference to the chunk identifier.
   *
   * @return The advertised chunk unique identifier
   */
  public PastryId getChunkId() {
    return chunkId;
  }

  /**
   * Returns the session id of the advertised chunk.
   *
   * @return The session id
   */
  public UUID getSessionId() {
    return sessionId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK_ADV;
  }

  /**
   * Creates a {@link ChunkRequest} message used to show interest in obtaining
   * the chunk from the advertising node.
   *
   * @return The {@link ChunkRequest} message
   */
  public ChunkRequest replyWithChunkReq() {
    return new ChunkRequest(getDestination(), getSource(), getSessionId(), getChunkId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return super.toString()+" Sid: "+sessionId+" Id: "+chunkId+" Timestamp: "+getTimeStamp();
  }
}