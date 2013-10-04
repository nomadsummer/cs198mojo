/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.google.code.peersim.starstream.protocol.messages;

import com.google.code.peersim.starstream.protocol.*;
import java.util.UUID;
import peersim.core.CommonState;

/**
 * Base abstract class for all the *-stream messages. Every *-stream message has
 * at least:
 * <ol>
 * <li>an <i>originator</i>, that is the node that sent the message for first</li>
 * <li>a <i>sender</i>, that is the node the message has been actually received from
 * (it is not the <i>originator</i> in case of forwarding)</li>
 * <li>a <i>destination</i>, that is the node the message is addressed to</li>
 * </ol>
 * Every concrete message class must also provide methods for replying to the current
 * message with another message, according to what the protocol prescribes.
 * <br><br>
 * <b>Note:</b> this class has a natural ordering that is inconsistent with equals.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public abstract class StarStreamMessage implements Comparable<StarStreamMessage> {

  /**
   * Classification of *-stream message types.
   *
   * @author frusso
   * @version 0.1
   * @since 0.1
   */
  public static enum Type {
    /**
     * This kind of message typically travels over unreliable transports and is
     * used for sending chunks to other nodes.
     */
    CHUNK {
      /**
       * {@inheritDoc}
       */
      @Override
      public int getEstimatedBandwidth() {
        //return 1024 + 180*8;
    	return 1000;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int getPriority() {
        return 6;
      }
    },
    /**
     * This kind of message typically travels over unreliable transports and is
     * used to reply to a {@link StarStreamMessage.Type#CHUNK} message to inform
     * the other node the chunk has been properly received.
     */
    CHUNK_OK {
      /**
       * {@inheritDoc}
       */
      @Override
      public int getEstimatedBandwidth() {
        //return 1024;
    	return 600;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int getPriority() {
        return 5;
      }
    },
    /**
     * This kind of message typically travels over unreliable transports and is
     * used to reply to a {@link StarStreamMessage.Type#CHUNK} message to inform
     * the other node the chunk has not been properly received.
     */
    CHUNK_KO {
      /**
       * {@inheritDoc}
       */
      @Override
      public int getEstimatedBandwidth() {
    	return 600;
        //return 1024;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int getPriority() {
        return 4;
      }
    },
    /**
     * This kind of message typically travels over reliable transports and is
     * used to inform other nodes (the <i>centers</i>) that a new chunk
     * has been received. Thus this message follows {@link StarStreamMessage.Type#CHUNK}
     * messages.
     */
    CHUNK_ADV {
      /**
       * {@inheritDoc}
       */
      @Override
      public int getEstimatedBandwidth() {
        //return 1024;
    	return 600;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int getPriority() {
        return 3;
      }
    },
    /**
     * This kind of message typically travels over reliable transports and is
     * used to reply to a {@link StarStreamMessage.Type#CHUNK_ADV} message. Such
     * a reply means that we are interested in receiving the advertised chunk.
     */
    CHUNK_REQ {
      /**
       * {@inheritDoc}
       */
      @Override
      public int getEstimatedBandwidth() {
        //return 1024;
    	return 600;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int getPriority() {
        return 2;
      }
    },
    /**
     * This kind of message typically travels over reliable transports and is
     * used to reply to a {@link StarStreamMessage.Type#CHUNK_REQ} message. Such
     * a reply means the requested chunk is not actually available.
     */
    CHUNK_MISSING {
      /**
       * {@inheritDoc}
       */
      @Override
      public int getEstimatedBandwidth() {
        //return 1024;
    	return 600;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int getPriority() {
        return 1;
      }
    };

    /**
     * Returns the estimated weight of the message in terms of bits.
     *
     * @return The estimated number of kbits
     */
    public abstract int getEstimatedBandwidth();

    /**
     * Returns the message type priority as far as the processing of delayed messages
     * by {@link StarStreamProtocol} instances is concerned.
     *
     * @return The message type priority
     */
    public abstract int getPriority();

    /**
     * Returns a human-readable description of the message type.
     */
    @Override
    public String toString() {
      return name();
    }
  }

  /**
   * Identifier of the message this message is related to. 128
   */
  private UUID correlationId;
  /**
   * The node that has to receive the message. 128
   */
  private StarStreamNode destination;
  /**
   * The number of hops the message has travelled. 64
   */
  private int hops;

  private int retries;
  /**
   * Unique message identifier. 128
   */
  private UUID messageId;
  /**
   * The node that originally sent the message for first. 128
   */
  private StarStreamNode originator;
  /**
   * The node the message has been received from. 128
   */
  private StarStreamNode source;
  /**
   * Message creation time. 128
   */
  private long timeStamp;

  /**
   * Internal constructor useful for subclassess only.
   *
   * @param src The sender (used to initialize the <i>originator</i> as well
   * @param dst The destination
   */
  protected StarStreamMessage(StarStreamNode src, StarStreamNode dst) {
    this.source = src;
    this.originator = src;
    this.destination = dst;
    messageId = UUID.randomUUID();
    hops = 0;
    retries = 0;
    timeStamp = CommonState.getTime();
  }

  /**
   * <b>Note:</b> this class has a natural ordering that is inconsistent with equals.
   * <br<br>
   * A {@link StarStreamMessage} is compared to another one considering firstly the
   * values of {@link Type#getPriority()}. If the two messages expose the same type,
   * they are compared considering their {@link StarStreamMessage#getTimeStamp()}
   * values.
   * <br<br>
   * {@inheritDoc}
   */
  @Override
  public int compareTo(StarStreamMessage o) {
    int res = 0;
    res = this.getType().getPriority() - o.getType().getPriority();
    if(res==0) {
      res = (int) (this.getTimeStamp() - o.getTimeStamp());
    }
    return res;
  }

  /**
   * Two {@link StarStreamMessage} instances are considered equivalent iff:
   * <ol>
   * <li>they have the same identifier</li>
   * <li>they have the same type</li>
   * </ol>
   * @param obj The other instance
   * @return {@link Boolean#TRUE} iff the two instances are logically equivalent,
   * {@link Boolean#FALSE} otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if(this==obj)
      return true;
    if(!(obj instanceof StarStreamMessage))
      return false;

    StarStreamMessage that = (StarStreamMessage)obj;
    return this.messageId.equals(that.messageId) &&
            this.getType().equals(that.getType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int hash = 17;
    hash = 31 * hash + this.messageId.hashCode();
    hash = 31 * hash + this.getType().hashCode();
    return hash;
  }

  /**
   * The actual message type.
   * @return The actual message type
   */
  public abstract Type getType();

  /**
   * Returns the message identifier of the message this message is related to.
   * @return The correlated message identifier
   */
  public UUID getCorrelationId() {
    return correlationId;
  }

  /**
   * The node that has to receive the message.
   * @return The destination
   */
  public StarStreamNode getDestination() {
    return destination;
  }

//  public int getEstimantedBandwidth() {
//    return getType().getEstimatedBandwidth();
//  }

  /**
   * Returns the number of hops the message has travelled so far.
   * @return The number of hops
   */
  public int getHops() {
    return hops;
  }

  /**
   * The unique immutable identifier associated with the message.
   * @return The message identifier
   */
  public UUID getMessageId() {
    return messageId;
  }

  /**
   * The node that originally sent the message.
   * @return The originator
   */
  public StarStreamNode getOriginator() {
    return originator;
  }

  /**
   * The node the message must be received from
   * @return The source
   */
  public StarStreamNode getSource() {
    return source;
  }

  /**
   * The message creation time.
   * @return The message creation time
   */
  public long getTimeStamp() {
    return timeStamp;
  }

  public void prepareForRetry() {
    increaseHops();
    increaseRetries();
    setTimeStamp(CommonState.getTime());
  }

  /**
   * The current retry time.
   * @return The current retry time
   */
  public int getRetries() {
    return retries;
  }

  /**
   * Add one to the current retry value.
   */
  public void increaseRetries() {
    retries++;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Type: "+getType()+" Src: "+
            (getSource()!=null ? getSource().getPastryId() : null)+
            " Dst: "+getDestination().getPastryId()+" Hops: "+getHops();
  }

  /**
   * Increase the current number of hops adding 1.
   */
  protected void increaseHops() {
    hops++;
  }

  /**
   * Sets the message identifier of the message this message is related to.
   * @param cid The correlated message identifier
   */
  protected void setCorrelationId(UUID cid) {
    this.correlationId = cid;
  }

  /**
   * Sets the destination of the message
   * @param destination The destination to set
   */
  protected void setDestination(StarStreamNode destination) {
    this.destination = destination;
  }

  /**
   * Sets the originator of the message.
   * @param originator The originator to set
   */
  protected void setOriginator(StarStreamNode originator) {
    this.originator = originator;
  }

  /**
   * Sets the source of the message
   * @param source The source to set
   */
  protected void setSource(StarStreamNode source) {
    this.source = source;
  }

  protected void setTimeStamp(long time) {
    this.timeStamp = time;
  }
}