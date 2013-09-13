/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.protocol.StarStreamNode;
import java.util.UUID;

/**
 * A {@link ChunkMissing} message must be used to signal a node that has issued
 * a {@link ChunkRequest} message that the desired chunk is not available at the
 * local node and thus cannot be provided.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkMissing extends ChunkAdvertisement {

  /**
   * Constructor. When creating a new instance, the specified source is also used to
   * initialize the message originator.
   *
   * @param src The sender
   * @param dst The destination
   * @param sessionId The sessionID
   * @param chunkId The chunk ID
   * @param correlationId The {@link ChunkRequest} message ID
   */
  ChunkMissing(StarStreamNode src, StarStreamNode dst, UUID sessionId, PastryId chunkId, UUID correlationId) {
    super(src, dst, sessionId, chunkId);
    setCorrelationId(correlationId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK_MISSING;
  }
}