package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.protocol.StarStreamNode;
import com.google.code.peersim.starstream.protocol.messages.StarStreamMessage.Type;
import java.util.UUID;

/**
 * This message is used to notify the sender of a {@link ChunkMessage} message that
 * the chunk has either not been received (i.e. due to timeout expiration) or
 * received currupted. The message must carry the unique identifier of the chunk
 * it refers to.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class ChunkKo extends ChunkRequest {

  /**
   * Constructor. When creating a new instance, the specified source is also used to
   * initialize the message originator.
   * 
   * @param src The sender
   * @param dst The destination
   * @param sessionId The session ID
   * @param chunkId The chunk ID
   * @param correlationId The correlation ID
   */
  ChunkKo(StarStreamNode src, StarStreamNode dst, UUID sessionId, PastryId chunkId, UUID correlationId) {
    super(src, dst, sessionId, chunkId);
    setCorrelationId(correlationId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return StarStreamMessage.Type.CHUNK_KO;
  }
}