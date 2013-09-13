/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.controls.ChunkUtils.Chunk;
import com.google.code.peersim.starstream.protocol.StarStreamNode;
import java.util.UUID;

/**
 * This message is used to express the interest in receiving a chunk that has
 * been advertised by means of a {@link ChunkAdvertisement} message.
 * 
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkRequest extends ChunkAdvertisement {

  private int retry = 0;

  /**
   * Constructor
   *
   * @param src The source
   * @param dst The destination
   * @param sessionId The session ID
   * @param chunkId The chunk ID
   */
  public ChunkRequest(StarStreamNode src, StarStreamNode dst, UUID sessionId, PastryId chunkId) {
    super(src, dst, sessionId, chunkId);
  }

  public int getRetry() {
    return retry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK_REQ;
  }

  public void increaseRetry() {
    retry++;
  }

  /**
   * If a node that receives a {@link ChunkRequest} is not able to provide the
   * inquiring node with the requested chunk, this method must be used to create
   * a {@link ChunkMissing} message.
   *
   * @return The {@link ChunkMissing} message
   */
  public ChunkMissing replyWithChunkMissing() {
    return new ChunkMissing(getDestination(), getSource(), getSessionId(), getChunkId(), getMessageId());
  }

  /**
   * If a node that receives a {@link ChunkRequest} is able to provide the
   * inquiring node with the requested chunk, this method must be used to create
   * a {@link ChunkMessage} message.
   *
   * @param chunk The chunk that has to be actually sent
   * @return The {@link ChunkMessage} message
   */
  public ChunkMessage replyWithChunkMessage(Chunk<?> chunk) {
    return new ChunkMessage(getDestination(), getSource(), chunk, 0, getMessageId());
  }
}