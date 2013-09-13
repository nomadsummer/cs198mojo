/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.starstream.protocol.*;
import com.google.code.peersim.starstream.controls.ChunkUtils.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import peersim.core.CommonState;

/**
 * This message is used to disseminate a new chunk into the network.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkMessage extends StarStreamMessage {

  /**
   * The chunk that has to be disseminated.
   */
  private Chunk chunk;

  /**
   * Constructor. When creating a new instance, the specified source is also used to
   * initialize the message originator.
   *
   * @param src The sender
   * @param dst The destination
   * @param chunk The chunk
   * @param retry The retry time
   */
  public ChunkMessage(StarStreamNode src, StarStreamNode dst, Chunk chunk, int retry) {
    super(src, dst);
    if(chunk==null) throw new IllegalArgumentException("The chunk cannot be 'null'");
    this.chunk = chunk;
  }

  /**
   * 
   * @param src
   * @param dst
   * @param chunk
   * @param retry
   * @param correlationId
   */
  ChunkMessage(StarStreamNode src, StarStreamNode dst, Chunk chunk, int retry, UUID correlationId) {
    super(src, dst);
    if(chunk==null) throw new IllegalArgumentException("The chunk cannot be 'null'");
    this.chunk = chunk;
    this.setCorrelationId(correlationId);
  }

  /**
   * Creates a {@link ChunkAdvertisement} message that must be used by nodes
   * that receive new chunks to advertise their presence to their neighbors.
   *
   * @param dst The destination
   * @return The {@link ChunkAdvertisement} message
   */
  public ChunkAdvertisement createChunkAdv(StarStreamNode dst) {
    return new ChunkAdvertisement(getDestination(), dst, chunk.getSessionId(), chunk.getResourceId());
  }

  /**
   * Creates a list of {@link ChunkAdvertisement} messages that must be used by nodes
   * that receive new chunks to advertise their presence to their neighbors.<br>
   * <b>NOTE:</b> the advertisement are ordered according to the ordering of the
   * provided destinations.
   *
   * @param dst The destinations
   * @return The {@link ChunkAdvertisement} message
   */
  public List<ChunkAdvertisement> createChunkAdvs(Set<StarStreamNode> dsts) {
    List<ChunkAdvertisement> advs = new ArrayList<ChunkAdvertisement>();
    for(StarStreamNode dst : dsts) {
      advs.add(createChunkAdv(dst));
    }
    return advs;
  }

  /**
   * Returns a reference to the chunk.
   * @return The chunk
   */
  public Chunk getChunk() {
    return chunk;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK;
  }

  /**
   * Tells whether the message has been sent by the source of the streaming.
   *
   * @return Whether the message has been sent by the source of the streaming
   */
  public boolean isFromSigma() {
    return getSource()==null;
  }

  /**
   * Creates a {@link ChunkKo} message required to signal the sending node
   * that the chunk was either currupted or the message did not arrived at all
   * i.e. due to timeout expiration.
   *
   * @return The {@link ChunkKo} message
   */
  public ChunkKo replyKo() {
    return new ChunkKo(getDestination(), getSource(), chunk.getSessionId(), chunk.getResourceId(), getMessageId());
  }

  /**
   * Creates a {@link ChunkOk} message required to signal the sending node
   * that the chunk has been properly received.
   *
   * @return The {@link ChunkOk} message
   */
  public ChunkOk replyOk() {
    return new ChunkOk(getDestination(), getSource(), chunk.getSessionId(), chunk.getResourceId(), getMessageId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return super.toString()+" Chunk: "+getChunk();
  }
}