/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.pastry.protocol.PastryJoinLsnrIfc.JoinedInfo;
import com.google.code.peersim.pastry.protocol.PastryProtocol;
import com.google.code.peersim.pastry.protocol.PastryProtocolListenerIfc;
import com.google.code.peersim.pastry.protocol.PastryResourceAssignLsnrIfc.ResourceAssignedInfo;
import com.google.code.peersim.pastry.protocol.PastryResourceDiscoveryLsnrIfc.ResourceDiscoveredInfo;
import com.google.code.peersim.starstream.controls.StarStreamSource;
import com.google.code.peersim.starstream.controls.ChunkUtils.Chunk;
import com.google.code.peersim.starstream.protocol.messages.ChunkAdvertisement;
import com.google.code.peersim.starstream.protocol.messages.ChunkKo;
import com.google.code.peersim.starstream.protocol.messages.ChunkMessage;
import com.google.code.peersim.starstream.protocol.messages.ChunkMissing;
import com.google.code.peersim.starstream.protocol.messages.ChunkOk;
import com.google.code.peersim.starstream.protocol.messages.ChunkRequest;
import com.google.code.peersim.starstream.protocol.messages.StarStreamMessage;
import com.google.code.peersim.starstream.protocol.messages.StarStreamMessage.Type;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import peersim.util.FileNameGenerator;

/**
 * Implementation of the *-Stream Protocol.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamProtocol implements EDProtocol, PastryProtocolListenerIfc {

  /**
   * Configurable timeout for *-Stream messages.
   */
  public static final String MSG_TIMEOUT = "timeOut";
  /**
   * Timeout value for *-Stream messages.
   */
  private int msgTimeout;
  /**
   * Configurable size for the *-Stream Store.
   */
  public static final String STAR_STORE_SIZE = "starStoreSize";
  /**
   * Size for the *-Stream Store.
   */
  private int starStoreSize;
  /**
   * Whether messages should be corruptable or not.
   */
  public static final String CORRUPTED_MESSAGES = "corruptedMessages";
  /**
   * Whether messages should be corruptable or not.
   */
  private boolean corruptedMessages;
  /**
   * Whether messages should be corruptable or not. Legal values are in [0..1].
   */
  public static final String CORRUPTED_MESSAGES_PROB = "corruptedMessagesProbability";
  /**
   * Whether messages should be corruptable or not.
   */
  private float corruptedMessagesProbability;
  /**
   * How many simulation-time units a node should try and send a chunk to another one.
   */
  public static final String MAX_CHUNK_RETRIES = "maxChunkRetries";
  /**
   * How many simulation-time units a node should try and send a chunk to another one.
   */
  private int maxChunkRetries;
  /**
   * Reliable transport protocol for *-Stream.
   */
  public static final String REL_TRANSPORT = "reliableTransport";
  /**
   * The protocol id assigned by the PeerSim runtime to the reliable transport instance.
   */
  private int reliableTransportPid;
  /**
   * Pastry protocol for *-Stream.
   */
  public static final String PASTRY_TRANSPORT = "pastryTransport";
  /**
   * Configurable file name for logging purposes.
   */
  public static final String LOG_FILE = "log";
  /**
   * Property for configuring whether the protocol should log its activity or not.
   */
  public static final String DO_LOG = "doLog";
  /**
   * Whether the protocol should log its activity or not.
   */
  private boolean doLog;
  /**
   * PeerSim property separator char.
   */
  private static final String SEPARATOR = ".";
  /**
   * This reference to the node associated with the current protocol instance.
   */
  private StarStreamNode owner;
  /**
   * The stream to log to.
   */
  private PrintStream stream;
  /**
   * The reference to the underlying {@link PastryProtocol} instance.
   */
  private PastryProtocol pastryProtocol;
  /**
   * This is the *-Stream local-store for storing chunks.
   */
  private StarStreamStore store;
  /**
   * Set of listeners configured to listen to protocol events.
   */
  private List<StarStreamProtocolListenerIfc> listeners = new ArrayList<StarStreamProtocolListenerIfc>();
  /**
   * Memory that associates a *-Stream message identifier with the message it refers to.
   * The identifier is meant to be matched to incoming messages' correlation-identifiers.
   */
  private Map<UUID, ChunkMessage> pendingChunkMessages = new HashMap<UUID, ChunkMessage>();
  /**
   * Memory that associates a *-Stream message identifier with the message it refers to.
   * The identifier is meant to be matched to incoming messages' correlation-identifiers.
   */
  private Map<UUID, ChunkRequest> pendingChunkRequests = new HashMap<UUID, ChunkRequest>();
  private int downStream;
  private int upStream;
  private int usedDownStream = 0;
  private int usedUpStream = 0;
  private SortedSet<StarStreamMessage> delayedInMessages = new TreeSet<StarStreamMessage>();
  private SortedSet<StarStreamMessage> delayedOutMessages = new TreeSet<StarStreamMessage>();
  private boolean aggressive;
  private long sentMessages = 0;
  private int unsentChunkMsgsDueToTimeout;
  /**
   * Counter of chunks received by means of the Pastry API.
   */
  private int chunksReceivedFromPastry;
  /**
   * Counter of chunks received by means of the StarStream API.
   */
  private int chunksReceivedFromStarStream;

  /**
   * Constructor. Sets up only those configuration parameters that can be set
   * by means of the PeerSim configuration file.
   *
   * @param prefix The configuration prefix
   */
  public StarStreamProtocol(String prefix) throws FileNotFoundException {
    msgTimeout = Configuration.getInt(prefix + SEPARATOR + MSG_TIMEOUT);
    starStoreSize = Configuration.getInt(prefix + SEPARATOR + STAR_STORE_SIZE);
    reliableTransportPid = Configuration.getPid(prefix + SEPARATOR + REL_TRANSPORT);
    doLog = Configuration.getBoolean(prefix + SEPARATOR + DO_LOG);
    if (doLog) {
      stream = new PrintStream(new FileOutputStream(new FileNameGenerator(Configuration.getString(prefix + SEPARATOR + LOG_FILE), ".log").nextCounterName()));
    }
    corruptedMessages = Configuration.getBoolean(prefix + SEPARATOR + CORRUPTED_MESSAGES);
    if (corruptedMessages) {
      corruptedMessagesProbability = (float) Configuration.getDouble(prefix + SEPARATOR + CORRUPTED_MESSAGES_PROB);
    }
    downStream = Configuration.getInt(prefix + SEPARATOR + "downStream");
    upStream = Configuration.getInt(prefix + SEPARATOR + "upStream");
    maxChunkRetries = Configuration.getInt(prefix + SEPARATOR + MAX_CHUNK_RETRIES);
    store = new StarStreamStore(starStoreSize);
    aggressive = Configuration.getBoolean(prefix + SEPARATOR + "aggressive");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object clone() {
    try {
      Object clone = super.clone();
      ((StarStreamProtocol) clone).owner = null;
      ((StarStreamProtocol) clone).pastryProtocol = null;
      ((StarStreamProtocol) clone).store = new StarStreamStore(starStoreSize);
      ((StarStreamProtocol) clone).listeners = new ArrayList<StarStreamProtocolListenerIfc>();
      ((StarStreamProtocol) clone).pendingChunkMessages = new HashMap<UUID, ChunkMessage>();
      ((StarStreamProtocol) clone).pendingChunkRequests = new HashMap<UUID, ChunkRequest>();
      ((StarStreamProtocol) clone).usedDownStream = 0;
      ((StarStreamProtocol) clone).usedUpStream = 0;
      ((StarStreamProtocol) clone).delayedInMessages = new TreeSet<StarStreamMessage>();
      ((StarStreamProtocol) clone).delayedOutMessages = new TreeSet<StarStreamMessage>();
      ((StarStreamProtocol) clone).sentMessages = 0;
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("Cloning failed. See nested exceptions, please.", e);
    }
  }

  /**
   * Returns the number of chunnks received by means of the Pastry API.
   * @return The number of chunnks received by means of the Pastry API.
   */
  public int getChunksReceivedFromPastry() {
    return chunksReceivedFromPastry;
  }

  /**
   * Returns the number of chunnks received by means of the StarStream API.
   * @return The number of chunnks received by means of the StarStream API.
   */
  public int getChunksReceivedFromStarStream() {
    return chunksReceivedFromStarStream;
  }
  
  //MOJO
  public int getUpload(){
	  return usedUpStream;
  }
  
  public int getDownload(){
	  return usedDownStream;
  }

  /**
   * Returns the number of unsent chunnks due to message timeout.
   * @return The number of unsent chunnks due to message timeout.
   */
  public int getUnsentChunkMsgsDueToTimeout() {
    return unsentChunkMsgsDueToTimeout;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void joined(JoinedInfo info) {
    log("[PASTRY-EVENT] " + info);
  }

  /**
   * Routes the event, that must be assignable to {@link StarStreamMessage}, to
   * the most appropriate handler.
   *
   * @param localNode The local node
   * @param thisProtocolId The protocol id
   * @param event The event
   */
  @Override
  public void processEvent(Node localNode, int thisProtocolId, Object event) {
    //System.out.println("[MOJO] " + CommonState.getTime() + " id: " + localNode.getID() + " upload: " + this.usedUpStream);
    //System.out.println("[MOJO] " + CommonState.getTime() + " id: " + localNode.getID() + " download: " + this.usedDownStream);  
	  // event-handling logic begins
    if (event instanceof StarStreamMessage) {
      // this is a known event, let's process it
      StarStreamMessage msg = (StarStreamMessage) event;
      
      if (updateUsedDownStream(msg)) {
        // there is enough input-stream to consume the message...
        processEvent(msg);
      } else {
        // there is not enough input-stream to consume the message: the message must
        // be cached for later use
        addToDelayedInMessages(msg);
      }
    } else {
      // an unknown event has been received
      throw new IllegalStateException("An event of type " + event.getClass() + " has been received, but I do not know how to handle it.");
    }
  }

  long getSentMessages() {
    return sentMessages;
  }

  /**
   * Starts the processing of both delayed input and output messages.
   */
  void processDelayedMessages() {
    StarStreamMessage[] inMsgs = new StarStreamMessage[delayedInMessages.size()];
    delayedInMessages.<StarStreamMessage>toArray(inMsgs);
    StarStreamMessage[] outMsgs = new StarStreamMessage[delayedOutMessages.size()];
    delayedOutMessages.<StarStreamMessage>toArray(outMsgs);

    int max = Math.max(inMsgs.length, outMsgs.length);
    for (int i = 0; i < max; i++) {
      log("[DELAYED MESSAGES] begin");
      // one from the ins...
      if (i < inMsgs.length) {
        // if the index is valid we try and process the message
        StarStreamMessage msg = inMsgs[i];
        if (updateUsedDownStream(msg)) {
          removeFromDelayedInMessages(msg);
          // since there is enough bandwidth we process the message
          processEvent(msg);
        }
      }
      // ... and from the outs
      if (i < outMsgs.length) {
        // if the index is valid we try and process the message
        StarStreamMessage msg = outMsgs[i];
        removeFromDelayedOutMessages(msg);
        send(msg);
      }
      log("[DELAYED MESSAGES] end");
    }
  }

  /**
   * Adds the given {@link StarStreamMessage} to the set of delayed input messages
   * for later processing.
   *
   * @param msg The message
   */
  private void addToDelayedInMessages(StarStreamMessage msg) {
    delayedInMessages.add(msg);
  }

  /**
   * Adds the given {@link StarStreamMessage} to the set of delayed output messages
   * for later processing.
   *
   * @param msg The message
   */
  private void addToDelayedOutMessages(StarStreamMessage msg) {
    delayedOutMessages.add(msg);
  }

  /**
   * Internal logic for processing incoming messages only if they have been already
   * accepted.
   *
   * @param msg The incoming message
   */
  private void processEvent(StarStreamMessage msg) {
    switch (msg.getType()) {
      case CHUNK: {
        handleChunk((ChunkMessage) msg);
        break;
      }
      case CHUNK_OK: {
        handleChunkOk((ChunkOk) msg);
        break;
      }
      case CHUNK_KO: {
        handleChunkKo((ChunkKo) msg);
        break;
      }
      case CHUNK_ADV: {
        handleChunkAdvertisement((ChunkAdvertisement) msg);
        break;
      }
      case CHUNK_REQ: {
        handleChunkRequest((ChunkRequest) msg);
        break;
      }
      case CHUNK_MISSING: {
        handleChunkMissing((ChunkMissing) msg);
        break;
      }
      default: {
        throw new IllegalStateException("A message of type " + msg.getType() + " has been received, but I do not know how to handle it.");
      }
    }
  }

  /**
   * Register the given listener for *-Stream protocol events.
   * 
   * @param lsnr The listener
   */
  public void registerStarStreamListener(StarStreamProtocolListenerIfc lsnr) {
    listeners.add(lsnr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resourceAssigned(ResourceAssignedInfo info) {
    // NOP for now
  }

  /**
   * When the underlying {@link PastryProtocol} instance discovers a resource
   * that is a {@link Chunk}, a {@link StarStreamProtocol} instance must:
   * <ol>
   * <li>peek that resource and store it locally (in the *-Stream Store} if it
   * has not been yet</li>
   * <li>advertise that resource to a set of randomly choosen neighbors</li>
   * </ol>
   * <b>Note:</b> no further routing is required since the {@link PastryProtocol}
   * instance is charge of that.
   *
   * {@inheritDoc}
   */
  @Override
  public void resourceDiscovered(ResourceDiscoveredInfo info) {
    log("[PASTRY-EVENT] " + info);
    Chunk<?> chunk = (Chunk<?>) info.getResource();
    handleChunkFromPastry(chunk);
  }

  /**
   * When the underlying {@link PastryProtocol} instance receives a resource
   * that is a {@link Chunk}, a {@link StarStreamProtocol} instance must:
   * <ol>
   * <li>peek that resource and store it locally (in the *-Stream Store} if it
   * has not been yet</li>
   * <li>advertise that resource to a set of randomly choosen neighbors</li>
   * </ol>
   * <b>Note:</b> no further routing is required since the {@link PastryProtocol}
   * instance is charge of that.
   *
   * {@inheritDoc}
   */
  @Override
  public void resourceReceived(ResourceReceivedInfo info) {
    log("[PASTRY-EVENT] " + info);
    Chunk<?> chunk = (Chunk<?>) info.getResource();
    handleChunkFromPastry(chunk);
  }

  /**
   * When the underlying {@link PastryProtocol} instance routes a resource
   * that is a {@link Chunk}, a {@link StarStreamProtocol} instance must:
   * <ol>
   * <li>peek that resource and store it locally (in the *-Stream Store} if it
   * has not been yet</li>
   * <li>advertise that resource to a set of randomly choosen neighbors</li>
   * </ol>
   * <b>Note:</b> no further routing is required since the {@link PastryProtocol}
   * instance is charge of that.
   *
   * {@inheritDoc}
   */
  @Override
  public void resourceRouted(ResourceRoutedInfo info) {
    log("[PASTRY-EVENT] " + info);
    Chunk<?> chunk = (Chunk<?>) info.getResource();
    handleChunkFromPastry(chunk);
  }

  /**
   * Removes the give listener from the set of registered ones.
   * 
   * @param lsnr The listener
   */
  public void unregisterStarStreamListener(StarStreamProtocolListenerIfc lsnr) {
    listeners.remove(lsnr);
  }

  /**
   * This method must be invoked to check whether there are expired {@link ChunkMessage}s}.
   * Each expired message gets removed from the memory and is sent again iff the
   * message has not been sent for the maximum amount of times yet.
   */
  void checkForTimeouts() {
    long currentTime = CommonState.getTime();
    checkForTimeoutsInPendingChunkMessages(currentTime);
    checkForTimeoutsInPendingChunkRequests(currentTime);
  }

  /**
   * Resends expired {@link ChunkMessage}s.
   *
   * @param currentTime Current simulated-time
   */
  private void checkForTimeoutsInPendingChunkMessages(long currentTime) {
    List<UUID> expired = new ArrayList<UUID>();
    // search
    for (Map.Entry<UUID, ChunkMessage> pending : pendingChunkMessages.entrySet()) {
      if (pending.getValue().getTimeStamp() + msgTimeout < currentTime) {
        // this message has expired!
        expired.add(pending.getKey());
      }
    }
    // removal & resending
    for (UUID id : expired) {
      ChunkMessage msg = pendingChunkMessages.remove(id);
      if (msg != null) {
        log("[TIMEOUT] (+"+(currentTime-(msgTimeout+msg.getTimeStamp()))+") " + msg);
        // resend iff the retry-time has not reached the configured max amount yet
        if (msg.getRetries() < maxChunkRetries) {
          // resend
          msg.prepareForRetry();
          send(msg);
        } else {
          log("[NOT SENT] " + MAX_CHUNK_RETRIES + " " + maxChunkRetries + " reached");
          unsentChunkMsgsDueToTimeout++;
        }
      }
    }
  }

  /**
   * Resends expired {@link ChunkRequests}s.
   *
   * @param currentTime Current simulated-time
   */
  private void checkForTimeoutsInPendingChunkRequests(long currentTime) {
    List<UUID> expired = new ArrayList<UUID>();
    // search
    for (Map.Entry<UUID, ChunkRequest> pending : pendingChunkRequests.entrySet()) {
      if (pending.getValue().getTimeStamp() + msgTimeout < currentTime) {
        // this message has expired!
        expired.add(pending.getKey());
      }
    }
    // removal & Pastry lookups
    for (UUID id : expired) {
      ChunkRequest msg = pendingChunkRequests.remove(id);
      if (msg != null) {
        log("[TIMEOUT] (+"+(currentTime-(msgTimeout+msg.getTimeStamp()))+") " + msg);
        // NOTE: there is no need to check whether the chuunk has already been
        // received since this is done by the method we are going to invoke
        owner.lookupResource(msg.getChunkId());
      }
    }
  }

  /**
   * Returns a reference to the local-store.
   *
   * @return The *-Store
   */
  StarStreamStore getStore() {
    return this.store;
  }

  /**
   * This method has to be invoked only once by the {@link StarStreamNode} instance that owns this
   * {@link StarStreamProtocol} instance to let the latter register itself as a listener for
   * Pastry protocol-events.
   *
   * @param pastry The {@link PastryProtocol} to register on for event notifications
   */
  void registerPastryListeners(PastryProtocol pastry) {
    pastryProtocol = pastry;
    pastryProtocol.registerListener(this);
  }

  /**
   * Tells the {@link StarStreamProtocol} instance that there has been
   * a new simulated-time tick and that both the outbound and inbound bandwiths
   * can be reset to their original levels.
   */
  void resetUsedBandwidth() {
    usedDownStream = 0;
    usedUpStream = 0;
  }

  /**
   * Starts searching for the specified chunk.
   *
   * @param starStreamSessionId The *-Stream session ID
   * @param chunkId The chunk ID
   */
  void searchForChunk(UUID starStreamSessionId, PastryId chunkId) {
//    Set<StarStreamNode> nodes = this.owner.getPastryProtocol().getNeighbors(1);
    Set<StarStreamNode> nodes = this.owner.getPastryProtocol().getNeighbors(this.availableOutDeg(Type.CHUNK_REQ));
    if (!nodes.isEmpty()) {
      StarStreamNode dst = nodes.iterator().next();
      ChunkRequest req = new ChunkRequest(owner, dst, starStreamSessionId, chunkId);
      if (send(req)) {
        // cache for on-timeout expiration retries
        rememberPendingChunkRequest(req);
      } else {
        // the message could not be sent due to bandwidth unavailabilty
        // it will be sent as soon as possible, thus NOP
      }
    } else {
      // go directly through Pastry
      owner.lookupResource(chunkId);
    }
  }

  /**
   * This method must be used only by {@link StarStreamNode} instances to tie
   * their identity to their own {@link StarStreamProtocol} instance.
   *
   * @param owner The owning node
   */
  void setOwner(StarStreamNode owner) {
    this.owner = owner;
  }

  /**
   * Advertises the existence of a new chunk to a selection of neighbors.
   * The number of neighbors is based on how many ougoing connections this
   * node is able to create.<br>
   * <b>Note:</b> advertisements travel over a reliable transport<br>
   * <b>Note:</b> as the *-Stream protocol says, this message must not be necessarly
   * followed by anyother message: a receiving node might reply or not according to
   * its interest in the advertised chunk
   *
   * @param msg The message containing the chunk that has to be advertised: can
   * be {@code null} iff {@code isOnPastryEvent} is {@link Boolean#TRUE}
   * @param isOnPastryEvent Must be {@link Boolean#TRUE} iff this method is invoked
   * since a new {@link Chunk} has been received as a Pastry event. If this is the case
   * the following {@link Chunk} parameter must be not {@code null}
   * @param chunk The {@link Chunk} that must be advertised: not {@code null} iff
   * {@code isOnPastryEvent} is {@link Boolean#TRUE}
   */
  private void advertiseChunk(ChunkMessage msg, boolean isOnPastryEvent, Chunk<?> chunk) {
    Set<StarStreamNode> neighbors;
    if (!aggressive) // use max concurrent upload capacity for chunks
    {
      neighbors = selectInNeighbors(StarStreamMessage.Type.CHUNK);
    } else // use max concurrent upload capacity for chunk_advs
    {
      neighbors = selectOutNeighbors(StarStreamMessage.Type.CHUNK_ADV);
    }
    List<ChunkAdvertisement> advs = null;
    if (isOnPastryEvent) {
      advs = ChunkAdvertisement.newInstancesFor(owner, neighbors, chunk);
    } else {
      advs = msg.createChunkAdvs(neighbors);
    }
    broadcastOverReliableTransport(advs);
  }

  /**
   * Tells how many input connections can be established before
   * exhausting the input bandwidth by receiving only messages of the given
   * message type.
   *
   * @param msgType The type of the message(s) the protocol would like to receive
   * @return The maximum number of connections available for the given message type
   */
  private int availableInDeg(Type type) {
    return (downStream - usedDownStream) / type.getEstimatedBandwidth();
  }

  /**
   * Tells how many output connections can be established before
   * exhausting the output bandwidth by sending only messages of the given
   * message type.
   *
   * @param msgType The type of the message(s) the protocol would like to send
   * @return The maximum number of connections available for the given message type
   */
  private int availableOutDeg(Type type) {
    return (upStream - usedUpStream) / type.getEstimatedBandwidth();
  }

  /**
   * Sends each message stored in the provided input over the configured reliable
   * transport.
   * 
   * @param msgs The messages
   */
  private void broadcastOverReliableTransport(List<? extends StarStreamMessage> msgs) {
    for (StarStreamMessage msg : msgs) {
      sendOverReliableTransport(msg);
    }
  }

  /**
   * Tells whether the message can be consumed or not. This is stated basing on
   * the {@link StarStreamProtocol#curruptedMessagesProbability} configured value.
   *
   * @param chunkMsg
   * @return Whether the message can be consumed or not
   */
  private boolean checkMessageIntegrity(ChunkMessage chunkMsg) {
    boolean res;
    if (corruptedMessages) {
      res = CommonState.r.nextFloat() < corruptedMessagesProbability;
    } else {
      res = true;
    }
    return res;
  }

  /**
   * When a new chunk is received, the receiving protocol instance has to do two
   * things "concurrently":
   * <ol>
   * <li>immediately reply the sending node with either a {@link ChunkOk} or
   * {@link ChunkKo} message (the integrity of the message can be verified by means
   * of the {@link StarStreamProtocol#checkMessageIntegrity(ChunkMessage)};<br><br>
   * <b>NOTE:</b> in case of corrupted message, we must abort the processing<br><br>
   * ask itself if it already owns the chunk; should it not be the case, store the 
   * chunk locally. In either case, it must now adverties the received chunk to
   * a set of <i>outDeg</i> neighbors. <i>outDeg</i> means the number of outgoing
   * connections the node can currently establish. How many outgoing connections
   * can be used at the moment is defined by the 
   * {@link StarStreamProtocol#availableOutDeg(com.google.code.peersim.starstream.protocol.StarStreamProtocol.NetworkOperation)}
   * method</li>
   * <li>iff the sender is the <i>source-node</i>, route the chunk over the Pastry network</li>
   * </ol>
   *
   * @param chunkMessage The message
   */
  private void handleChunk(ChunkMessage chunkMessage) {
    log("[RCV] " + chunkMessage);
    if (!chunkMessage.getChunk().isExpired()) {
      if (checkMessageIntegrity(chunkMessage)) {
        // remove from the pending requests
        removeFromPendingChunkRequests(chunkMessage);
        // send OK and proceede
        handleChunk_SendOK(chunkMessage);
        // locally save the chunk
        storeIfNotStored(chunkMessage.getChunk());
        // advertise the new chunk
        advertiseChunk(chunkMessage, false, null);
        if (chunkMessage.isFromSigma()) {
          // route the resource over the Pastry network
          owner.publishResource(chunkMessage.getChunk());
        }
      } else {
        // send KO and abort
        handleChunk_SendKO(chunkMessage);
      }
    } else {
      log("[*** EXPIRED ***] message " + chunkMessage + " contains a chunk that exceeded its TTL: not delivered!");
    }
  }

  /**
   * When a node receives a {@link ChunkAdvertisement} it has to:
   * <ol>
   * <li>check whether the advertised chunk is already locally stored or not</li>
   * <li>should the chunk have not been received yet, the node has to issue a
   * {@link ChunkRequest} message to the advertising node, showing interest in
   * receiving that chunk</li>
   * </ol>
   * <b>Note:</b> both the {@link ChunkAdvertisement} and the {@link ChunkRequest}
   * travel over the reliable transport available to each {@link StarStreamProtocol}
   * instance.
   *
   * @param chunkAdvertisement The chunk advertisement
   */
  private void handleChunkAdvertisement(ChunkAdvertisement chunkAdvertisement) {
    log("[RCV] " + chunkAdvertisement);
    if (!store.isStored(chunkAdvertisement.getSessionId(), chunkAdvertisement.getChunkId())) {
      // the chunk is not locally available, thus we need to reply to the advertising
      // node with a chunk request message and wait for the chunk to arrive
      ChunkRequest chunkReq = chunkAdvertisement.replyWithChunkReq();
      send(chunkReq);
    } else {
      // the chunk is already stored in the local *-Stream store, thus there is no
      // need and doing anything else
      // NOP
    }
  }

  /**
   * @see StarStreamProtocol#resourceRouted(com.google.code.peersim.pastry.protocol.PastryResourceAssignLsnrIfc.ResourceRoutedInfo)
   * @see StarStreamProtocol#resourceReceived(com.google.code.peersim.pastry.protocol.PastryResourceAssignLsnrIfc.ResourceReceivedInfo)
   * @see StarStreamProtocol#resourceDiscovered(com.google.code.peersim.pastry.protocol.PastryResourceDiscoveryLsnrIfc.ResourceDiscoveredInfo)
   */
  private void handleChunkFromPastry(Chunk<?> chunk) {
    if (storeIfNotStored(chunk)) {
      chunksReceivedFromPastry++;
      advertiseChunk(null, true, chunk);
    }
  }

  /**
   * When a {@link ChunkKo} message is received, we know that the sender of that
   * message was not able (for whichever reason) to properly receive the chunk the
   * message refers to.<br>
   * For this reason, the protocol states that a node receiving a {@link ChunkKo}
   * message must:
   * <ol>
   * <li>check to see whether the chunk is locally available or not</li>
   * <li>if the chunk was actually available the node should send it using a {@link ChunkMessage},
   * otherwise a {@link ChunkMissing} message must be sent</li>
   * </ol>
   *
   * @param chunkKo The KO message
   */
  private void handleChunkKo(ChunkKo chunkKo) {
    removeFromPendingChunks(chunkKo);
    tryAndSendChunk(chunkKo);
  }

  /**
   * A {@link StarStreamProtocol} instance can receive a {@link ChunkMissing} message
   * only in response to a previously issued {@link ChunkRequest} message. Obviously,
   * a {@link ChunkMissing} message reports that a requested chunk could not be provided
   * by the inquired node. Should this happen, the protocol prescribes that the chunk
   * must be searched for using the underlying {@link PastryProtocol} instance, issueing
   * a {@link PastryProtocol#lookupResource(com.google.code.peersim.pastry.protocol.PastryId, int)}.
   *
   * @param chunkMissing The message
   */
  private void handleChunkMissing(ChunkMissing chunkMissing) {
    log("[RCV] " + chunkMissing);
    // the only thing we need to do is launching a Pastry resource lookup operation
    // once and if the resource is found, this protocol instance will be notified by
    // the underlying Pastry implementation and the resource will be finally stored
    // in the local *-Stream store
    removeFromPendingChunkRequests(chunkMissing);
    pastryProtocol.lookupResource(chunkMissing.getChunkId());
  }

  /**
   * When a {@link ChunkOk} message is received, it means that a node we previously
   * sent a {@link ChunkMessage} has successfully received the chunk. As far as the
   * *-Stream protocol is concerned, at this point any other activity is foreseen
   * for the node that received such a message.<br>
   * <b>Note:</b> since {@link ChunkMessage}s are sent over an unreliable transport,
   * upon reception of a {@link ChunkOk} message it is necessary purging the memory
   * of {@link ChunkMessage}s pending acks to avoid useless retransmissions.
   *
   * @param chunkOk The OK message
   */
  private void handleChunkOk(ChunkOk chunkOk) {
    log("[RCV] " + chunkOk);
    removeFromPendingChunks(chunkOk);
  }

  /**
   * A request for a chunk, that is a {@link ChunkRequest} message can be received:
   * <ol>
   * <li>either in response to a previously issued {@link ChunkAdvertisement}</li>
   * <li>or as the effect of a proactive search initiated by a *-Stream node</li>
   * </ol>
   * In either case the receiving node has to:
   * <ol>
   * <li>check whether the requested chunk is locally available or not</li>
   * <li>if the chunk is available, reply with a {@link ChunkMessage} message;<br>
   * if the chunk is not available, reply with a {@link ChunkMissing} message</li>
   * </ol>
   * <b>Note:</b> the {@link ChunkMessage} has to travel over the unreliable transport
   * while the {@link ChunkMissing} has to travel over the reliable transpor.
   *
   * @param chunkRequest
   */
  private void handleChunkRequest(ChunkRequest chunkRequest) {
    tryAndSendChunk(chunkRequest);
  }

  /**
   * Reply to the node that just sent the input {@link ChunkMessage} with a
   * {@link ChunkKo} message stating that the chunk has not been properly received.<br>
   * <b>Note:</b> this message is sent over a reliable transport.<br>
   * <b>Note:</b> according to the protocol specification, this message could be followed
   * by a new {@link ChunkMessage} carrying the chunk that could have not been properly
   * received.
   *
   * @param chunkMessage The message to reply to
   */
  private void handleChunk_SendKO(ChunkMessage chunkMessage) {
    // by convention, should the sender be the source, we avoid sending the
    // ack over the simulated transport otherwise we do
    if (chunkMessage.isFromSigma()) {
      StarStreamSource.chunkKo(chunkMessage.getChunk().getResourceId());
    } else {
      ChunkKo ko = chunkMessage.replyKo();
      send(ko);
    }
  }

  /**
   * Reply to the node that just sent the input {@link ChunkMessage} with a
   * {@link ChunkOk} message stating that the chunk has been properly received.<br>
   * <b>Note:</b> this message is sent over a reliable transport.<br>
   * <b>Note:</b> the protocol specifies this message must not be followed by anyother
   * message.<br>
   *
   * @param chunkMessage The message to reply to
   */
  private void handleChunk_SendOK(ChunkMessage chunkMessage) {
    // by convention, should the sender be the source, we avoid sending the
    // ack over the simulated transport
    // otherwise we do
    if (chunkMessage.isFromSigma()) {
      StarStreamSource.chunkOk(chunkMessage.getChunk().getResourceId());
    } else {
      ChunkOk ok = chunkMessage.replyOk();
      send(ok);
    }
  }

  /**
   * Logs the given message appending a new-line to the input parameter.
   * @param msg The log message
   */
  private void log(String msg) {
    if(doLog)
      stream.print(CommonState.getTime() + ") " + msg + "\n");
  }

  /**
   * Notifies the registered listeners about the availability of a new chunk
   * in the *-Stream local store.
   *
   * @param chunk The new chunk
   */
  private void notifyChunkStoredToListeners(Chunk<?> chunk) {
    if (listeners != null) {
      for (StarStreamProtocolListenerIfc lsnr : listeners) {
        lsnr.notifyNewChunk(chunk);
      }
    }
  }

  /**
   * Adds the given {@link ChunkMessage} to the set of {@link ChunkMessage}s waiting
   * for an ack.
   * 
   * @param chunkMessage The message
   */
  private void rememberPendingChunk(ChunkMessage chunkMessage) {
    pendingChunkMessages.put(chunkMessage.getMessageId(), chunkMessage);
  }

  /**
   * Adds the given {@link ChunkRequest} to the set of {@link ChunkRequests}s waiting
   * for an ack.
   *
   * @param chunkMessage The message
   */
  private void rememberPendingChunkRequest(ChunkRequest req) {
    pendingChunkRequests.put(req.getMessageId(), req);
  }

  /**
   * Removes the given message from the delayed incoming messages.
   * 
   * @param msg The message
   */
  private void removeFromDelayedInMessages(StarStreamMessage msg) {
    delayedInMessages.remove(msg);
  }

  /**
   * Removes the given message from the delayed outgoing messages.
   *
   * @param msg The message
   */
  private void removeFromDelayedOutMessages(StarStreamMessage msg) {
    delayedOutMessages.remove(msg);
  }

  /**
   * Removes the {@link ChunkMessage}, identified by the correlation-id found in the
   * input message, from the set of {@link ChunkMessage}s waiting for an ack.
   *
   * @param msg The message
   */
  private void removeFromPendingChunks(StarStreamMessage msg) {
    pendingChunkMessages.remove(msg.getCorrelationId());
  }

  /**
   * Removes the {@link ChunkRequest}, identified by the correlation-id found in the
   * input message, from the set of {@link ChunkRequest}s waiting for an ack.
   *
   * @param msg The message
   * @return The stored chunk reuqest or {@code null}
   */
  private ChunkRequest removeFromPendingChunkRequests(StarStreamMessage msg) {
    return pendingChunkRequests.remove(msg.getCorrelationId());
  }

  /**
   * Randomly selects neighbors according to Pastry's <i>neighbor</i> definition.
   *
   * @param type The type of the message(s) the protocol would like to send
   * @return Zero or more Pastry neighbors
   */
  private Set<StarStreamNode> selectOutNeighbors(StarStreamMessage.Type type) {
    int availableOutConnections = availableOutDeg(type);
    return pastryProtocol.getNeighbors(availableOutConnections);
  }

  /**
   * Randomly selects neighbors according to Pastry's <i>neighbor</i> definition.
   *
   * @param type The type of the message(s) the protocol would like to send
   * @return Zero or more Pastry neighbors
   */
  private Set<StarStreamNode> selectInNeighbors(StarStreamMessage.Type type) {
    int availableInConnections = availableInDeg(type);
    return pastryProtocol.getNeighbors(availableInConnections);
  }

  /**
   * Generic send method that must be used in place of anyother {@code sendXxx} method.
   *
   * @param msg The message
   * @return Whether the message has been sent ({@link Boolean#TRUE}) or delayed ({@link Boolean#FALSE})
   */
  private boolean send(StarStreamMessage msg) {
    boolean sent = false;
    if (StarStreamMessage.Type.CHUNK.equals(msg.getType())) {
      sent = sendChunkMessage((ChunkMessage) msg);
    } else {
      // send over reliable transport
      sent = sendOverReliableTransport(msg);
    }
    return sent;
  }

  /**
   * When sending a {@link ChunkMessage} this method must be used to avoid
   * unnecessary and dangerous code duplications.
   *
   * @param msg The message
   * @return Whether the message has been sent ({@link Boolean#TRUE}) or delayed ({@link Boolean#FALSE})
   */
  private boolean sendChunkMessage(ChunkMessage msg) {
    boolean sent = false;
    if (!msg.getChunk().isExpired()) {
      // send over unrel. transport
      if (sendOverUnreliableTransport(msg)) {
        // add to chunks waiting for ack
        rememberPendingChunk(msg);
        sent = true;
      } else {
        // NOP, the message has been surely delayed
      }
    } else {
      log("[*** EXPIRED ***] chunk "+msg.getChunk()+" expired, chunk message not sent");
    }
    return sent;
  }

  /**
   * Send the input {@link StarStreamMessage} over the configured reliable transport.
   *
   * @param msg The message
   * @return Whether the message has been sent ({@link Boolean#TRUE}) or delayed ({@link Boolean#FALSE})
   */
  private boolean sendOverReliableTransport(StarStreamMessage msg) {
    boolean sent = false;
    if (updateUsedUpStream(msg)) {
      Transport t = (Transport) owner.getProtocol(reliableTransportPid);
      t.send(msg.getSource(), msg.getDestination(), msg, owner.getStarStreamPid());
      log("[SND] " + msg);
      sent = true;
      sentMessages++;
    } else {
      // up-stream has been exhausted, the message must be delayed or dropped
      addToDelayedOutMessages(msg);
    }
    return sent;
  }

  /**
   * Send the input {@link StarStreamMessage} over the configured unreliable transport.
   *
   * @param msg The message
   * @return Whether the message has been sent ({@link Boolean#TRUE}) or delayed ({@link Boolean#FALSE})
   */
  private boolean sendOverUnreliableTransport(StarStreamMessage msg) {
    boolean sent = false;
    if (updateUsedUpStream(msg)) {
      Transport t = owner.getStarStreamTransport();
      t.send(msg.getSource(), msg.getDestination(), msg, owner.getStarStreamPid());
      log("[SND] " + msg);
      sent = true;
      sentMessages++;
    } else {
      // up-stream has been exhausted, the message must be delayed or dropped
      addToDelayedOutMessages(msg);
    }
    return sent;
  }

  /**
   * Adds the given chunk to the local *-Stream Store iff that chunk is not yet
   * in the store.
   * 
   * @param chunk The chunk
   */
  private boolean storeIfNotStored(Chunk<?> chunk) {
    boolean stored = store.addChunk(chunk);
    if (stored) {
      chunksReceivedFromStarStream++;
      // the chunk has been added to the local store
      notifyChunkStoredToListeners(chunk);
    } else {
      // the chunk has not been added to the local store: this means it was
      // already there
      // NOP
    }
    return stored;
  }

  /**
   * Utility method useful upon reception of either a {@link ChunkRequest} or a
   * {@link ChunkKo} message that:
   * <ol>
   * <li>checks whether the requested chunk is locally available or not</li>
   * <li>if the chunk was available, reply with a {@link ChunkMessage} message;<br>
   * if the chunk was not available, reply with a {@link ChunkMissing} message</li>
   * </ol>
   * <b>Note:</b> the {@link ChunkMessage} has to travel over the unreliable transport
   * while the {@link ChunkMissing} has to travel over the reliable transpor.
   *
   * @param req The request for a chunk (retransmission)
   */
  private void tryAndSendChunk(ChunkRequest req) {
    log("[RCV] " + req);
    Chunk<?> chunk = store.getChunk(req.getSessionId(), req.getChunkId());
    if (chunk != null) {
      // the chunk is locally available, let's reply with a chunk message
      ChunkMessage chunkMessage = req.replyWithChunkMessage(chunk);
      send(chunkMessage);
    } else {
      ChunkMissing chunkMissing = req.replyWithChunkMissing();
      send(chunkMissing);
    }
  }

  /**
   * This method must be invoked for each receive operation just to increase
   * the amout of used incoming network traffic for the current protocol
   * instance.
   *
   * @param msg The message that has been sent
   * @return {@link Boolean#FALSE} is the down-stream has been exahusted,
   * {@link Boolean#TRUE} otherwise
   */
  private boolean updateUsedDownStream(StarStreamMessage msg) {
    boolean proceed = false;
    if(usedDownStream+msg.getType().getEstimatedBandwidth()<=downStream) {
      usedDownStream+=msg.getType().getEstimatedBandwidth();
      proceed = true;
    }
    return proceed;
//    return true;
  }

  /**
   * This method must be invoked after every send operation just to increase
   * the amout of used outgoing network traffic for the current protocol
   * instance.
   *
   * @param msg The message that has been sent
   * @return {@link Boolean#FALSE} is the up-stream has been exahusted,
   * {@link Boolean#TRUE} otherwise
   */
  private boolean updateUsedUpStream(StarStreamMessage msg) {
    boolean proceed = false;
    if(usedUpStream+msg.getType().getEstimatedBandwidth()<=upStream) {
      usedUpStream+=msg.getType().getEstimatedBandwidth();
      proceed = true;
    }
    return proceed;
//    return true;
  }
}