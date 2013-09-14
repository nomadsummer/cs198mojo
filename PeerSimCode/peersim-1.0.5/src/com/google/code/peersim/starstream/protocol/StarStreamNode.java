/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.pastry.protocol.PastryNode;
import com.google.code.peersim.pastry.protocol.PastryProtocol;
import com.google.code.peersim.starstream.controls.ChunkUtils;
import com.google.code.peersim.starstream.controls.ChunkUtils.Chunk;
import com.google.code.peersim.starstream.controls.StarStreamSource;
import java.io.FileNotFoundException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.transport.Transport;
import peersim.util.IncrementalStats;

/**
 * This class represents a node that leverages both the *-Stream and the Pastry
 * protocols to take part to a P2P streaming event.<br>
 * Extending {@link PastryNode} this class can take advantage of all the features
 * and control classes available for {@link PastryNode}.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamNode extends PastryNode implements StarStreamProtocolListenerIfc {

  /**
   * Configuration parameter key that ties a *-Stream node to its *-Stream protocol.
   */
  public static final String STAR_STREAM = "starstream";
  /**
   * The protocol identifier assigned to the *-Stream protocol by the PeerSim runtime.
   */
  private int STAR_STREAM_PID;
  /**
   * Separator char used for PeersSim-related configuration properties.
   */
  private static final String SEPARATOR = ".";
  private int MIN_CONTIGUOUS_CHUNKS_IN_BUFFER;
//  private long START_STREAMING_TIME;
  private int START_STREAMING_TIMEOUT;
  private int WAIT_BETWEEN_FORCES;
  private long lastForce;
  private boolean aggressive;
//  private long whenPlaybackStarted;
  private IncrementalStats perceivedChunkDeliveryTimes;
  private int advance;
  private int chunkPlaybackLength;
  // useful to know which chunks were requested for but not discovered, at the end of the simulation
  private Set<Integer> issuedChunkRequests;
  private SortedSet<Integer> deliveredChunks;
  private List<Integer> chunkRequestsForSeqIdsWithoutPastryIdYet;
//  private int lastPlayedChunkSeqId;
  private StarStreamPlayer player;
  private int totalChunks;
  private Long streamingStartTime;

  /**
   * Default PeerSim-required constructor.
   *
   * @param prefix The prefix for accessing configuration properties
   * @throws java.io.FileNotFoundException Raised whenever it is not possible creating the log file
   */
  public StarStreamNode(String prefix) throws FileNotFoundException {
    super(prefix);
    STAR_STREAM_PID = Configuration.getPid(prefix + SEPARATOR + STAR_STREAM);
    // at this point the protocl stack for the current node must have been already
    // initialized by the PeerSim runtime, so this is the right time for takeing
    // references to Pastry and *-Stream and tight the two instance enabling the
    // latter to receive notifications from the former
    MIN_CONTIGUOUS_CHUNKS_IN_BUFFER = Configuration.getInt(prefix + SEPARATOR + "minContiguousChunksInBuffer");
//    START_STREAMING_TIME = Configuration.getLong(prefix + SEPARATOR + "startStreaming");
    START_STREAMING_TIMEOUT = (int) Math.ceil(Configuration.getDouble(prefix + SEPARATOR + "startStreamingTimeout"));
    WAIT_BETWEEN_FORCES = Configuration.getInt(prefix + SEPARATOR + "waitBetweenForces");
    aggressive = Configuration.getBoolean(prefix + SEPARATOR + "aggressive");
    advance = Configuration.getInt(prefix + SEPARATOR + "advance");
    chunkPlaybackLength = Configuration.getInt(prefix + SEPARATOR + "chunkPlaybackLength");
    totalChunks = Configuration.getInt(prefix + SEPARATOR + "totalChunks");
    init();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object clone() {
    StarStreamNode clone = (StarStreamNode) super.clone();
    clone.init();
    return clone;
  }

  public int countMissingChunks() {
    return issuedChunkRequests.size();
  }

  /**
   * Returns the number of chunnks received by means of the Pastry API.
   * @return The number of chunnks received by means of the Pastry API.
   */
  public int getChunksReceivedFromPastry() {
    return getStarStreamProtocol().getChunksReceivedFromPastry();
  }

  /**
   * Returns the number of chunnks received by means of the StarStream API.
   * @return The number of chunnks received by means of the StarStream API.
   */
  public int getChunksReceivedFromStarStream() {
    return getStarStreamProtocol().getChunksReceivedFromStarStream();
  }

  public List<Integer> getUnplayedChunks() {
    return  player.getMissedChunks();
  }

  public double getPerceivedAvgChunkDeliveryTime() {
    return perceivedChunkDeliveryTimes.getAverage();
  }

  public double getPerceivedMaxChunkDeliveryTime() {
    double max;
    if (perceivedChunkDeliveryTimes.getN() == 0) {
      max = Double.MIN_VALUE;
    } else {
      max = perceivedChunkDeliveryTimes.getMax();
    }
    return max;
  }

  public double getPercentageOfUnplayedChunks() {
    return 100 * getUnplayedChunks().size() / totalChunks;
  }

  public List<Integer> getPlayedChunks() {
    return player.getPlayedChunks();
  }

  public StarStreamPlayer getPlayer() {
    return player;
  }

  public long getSentMessages() {
    return getStarStreamProtocol().getSentMessages();
  }

  /**
   * The PeerSim-assigned *-Stream protocol identifier.
   *
   * @return The PeerSim-assigned *-Stream protocol identifier.
   */
  public int getStarStreamPid() {
    return STAR_STREAM_PID;
  }

  /**
   * Returns a reference to the *-Store owned by this node.
   *
   * @return The *-Store
   */
  public StarStreamStore getStore() {
    return getStarStreamProtocol().getStore();
  }

  public int getUnsentChunkMsgsDueToTimeout() {
    return getStarStreamProtocol().getUnsentChunkMsgsDueToTimeout();
  }

  public long getWhenPlaybackStarted() {
    return player.getWhenPlaybackStarted();
  }

  public boolean hasStartedPlayback() {
    return player.isStarted();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void notifyNewChunk(Chunk<?> chunk) {
    log("[*-STREAM] node " + this.getPastryId() + " has stored resource " + chunk);
    updateLocalStats(chunk);
    removeFromIssuedChunkRequests(chunk.getSequenceId());
    addToDeliveredChunks(chunk.getSequenceId());
    if (!player.isStarted()) {
      checkIfPlaybackIsAllowed();
    }
  }

  /**
   * Tells the node to start processing potentially delayed messages.
   */
  public void processDelayedMessages() {
    if (isJoined()) {
      getStarStreamProtocol().processDelayedMessages();
    }
  }

  /**
   * Tells the associated {@link StarStreamProtocol} instance that there has been
   * a new simulated-time tick and that both the outbound and inbound bandwiths
   * can be reset to their original levels.
   */
  public void resetUsedBandwidth() {
    getStarStreamProtocol().resetUsedBandwidth();
  }
  
  //MOJO
  public void getBandwidth(){
	int thisup = getStarStreamProtocol().getUpload();
	int thisdown = getStarStreamProtocol().getDownload();
	
	//System.out.println("[MOJO] " + CommonState.getTime() + " id: " + this.getID() + " upload: " + thisup);
    //System.out.println("[MOJO] " + CommonState.getTime() + " id: " + this.getID() + " download: " + thisdown);
  }

  public void streamingStartsAt(long start) {
    streamingStartTime = start;
  }

  public void tick() {
    if (isJoined() && streamingStartTime!=null) {
      checkForStarStreamTimeouts();
      checkForStartStreamingTimeout();
      proactiveSearch();
      player.tick();
      System.out.println("[MOJO] "+ CommonState.getTime()+" "+ this.getID() + " " + player.getPBLength());
      System.out.println("[MOJO] PlayedChunks: "+ this.getPlayedChunks().size());
      System.out.println("[MOJO] MissedChunks: "+ this.getUnplayedChunks().size());
      if(this.getUnplayedChunks().size() > 0){
    	  System.out.println("[MOJO] PlayedChunksList: "+ this.getPlayedChunks());
          System.out.println("[MOJO] MissedChunksList: "+ this.getUnplayedChunks());  
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return super.toString() + "\n" + getStore();
  }

  /**
   * Returns a reference to the configure <i>unreliable</i> transport for the
   * *-Stream protocol.
   *
   * @return The unreliable transport
   */
  Transport getStarStreamTransport() {
    return (Transport) getProtocol(FastConfig.getTransport(STAR_STREAM_PID));
  }

  /**
   * Returns a reference to this node's assigned {@link StarStreamProtocol} instance.
   * @return The {@link StarStreamProtocol} instance
   */
  protected StarStreamProtocol getStarStreamProtocol() {
    return (StarStreamProtocol) getProtocol(STAR_STREAM_PID);
  }

  /**
   * Tells the *-Stream protocol to check for expired messages that were waiting
   * for acks/nacks and behave consequently.
   */
  private void checkForStarStreamTimeouts() {
    getStarStreamProtocol().checkForTimeouts();
  }

  private void checkForStartStreamingTimeout() {
    if (!player.isStarted() && CommonState.getTime()>=(streamingStartTime+START_STREAMING_TIMEOUT)) {
      if (CommonState.getTime()>lastForce+WAIT_BETWEEN_FORCES) {
        // driiin!!! timeout expired
        // start proactive search (pull) for those chunks required to fill in
        // the buffer
        lastForce = CommonState.getTime();
        List<Integer> missingChunkIds = collectMissingChunkIds();
        UUID sessionId = StarStreamSource.getStarStreamSessionId();
        for (Integer id : missingChunkIds) {
          if(addToIssuedChunkRequests(id)) {
            issueChunkRequest(sessionId, id);
          }
        }
      }
    }
  }

  private void issueChunkRequest(UUID sessionId, int nextChunkSeqId) {
    PastryId chunkId = ChunkUtils.getChunkIdForSequenceId(sessionId, nextChunkSeqId);
    if (chunkId != null) {
      log("[*-STREAM] node " + this.getPastryId() + " starts looking for chunk(" + nextChunkSeqId + ") " + chunkId);
      getStarStreamProtocol().searchForChunk(sessionId, chunkId);
    } else {
      chunkRequestsForSeqIdsWithoutPastryIdYet.add(nextChunkSeqId);
      log("[*-STREAM] WARN node " + this.getPastryId() + " No one knows anything about chunk " + nextChunkSeqId);
    }
  }

  private SortedSet<Integer> getMissingChunkSeqIdsLessThan(int seqId) {
    SortedSet<Integer> missings = new TreeSet<Integer>();
    // the lowest received chunk seq-id (if any)
    int first;
    if (!deliveredChunks.isEmpty()) {
      first = deliveredChunks.first();
    } else {
      // any chunk has been delivered yet: let's assume first is -1
      first = -1;
    }
    // let's gather the missing seq-ids
    if (first < seqId) {
      for (int i = seqId - 1; i > first; i--) {
        if (!deliveredChunks.contains(i) && !chunkRequestsForSeqIdsWithoutPastryIdYet.contains(i)) {
          missings.add(i);
        }
      }
    }
    // and return them
    return missings;
  }

  private void proactiveSearch() {
    if (player.isStarted() && (CommonState.getTime() > streamingStartTime+START_STREAMING_TIMEOUT)) {
      processDelayedChunkRequests();
      scheduleNextChunkRequest();
    }
  }

  private void processDelayedChunkRequests() {
    UUID sessionId = StarStreamSource.getStarStreamSessionId();
    Integer[] pcrs = chunkRequestsForSeqIdsWithoutPastryIdYet.toArray(new Integer[chunkRequestsForSeqIdsWithoutPastryIdYet.size()]);
    chunkRequestsForSeqIdsWithoutPastryIdYet.clear();
    for (int pcr : pcrs) {
      issueChunkRequest(sessionId, pcr);
    }
  }

  private void scheduleNextChunkRequest() {
    long currentTime = CommonState.getTime();
    double avgObservedDeliveryTime = (perceivedChunkDeliveryTimes.getN() == 0) ? 0 : perceivedChunkDeliveryTimes.getAverage();
    double num = currentTime + avgObservedDeliveryTime - player.getWhenPlaybackStarted() + advance;
    int nextChunkSeqId = Double.valueOf(Math.floor(num / chunkPlaybackLength)).intValue();
    if (StarStreamSource.isSeqIdLegal(nextChunkSeqId) && !hasBeenDelivered(nextChunkSeqId)) {
      // once the next seq-id has been computed we have to:
      // 1. store somewhere that the i-th chunk has been scheduled for search right now
      // 2. start searching for that chunk iff this is the very first time we scheduled it
      // NOTE: pro-actively searching for a chunk entails two steps:
      // 1. ask some neighbors if anyone of them actually has the chunk
      // 2. either in the event of nacks or in case of req-timeout expiration, issue
      //    a Pastry lookup for that chunk: the Pastry lookup already has its own
      //    configurable retries
      // Thus, according to the two observations above, it is not necessary implementing
      // at this level any kind of resubmission logic for chunk requests
      SortedSet<Integer> missingChunkSeqIds = getMissingChunkSeqIdsLessThan(nextChunkSeqId);
      missingChunkSeqIds.add(nextChunkSeqId);
      UUID sessionId = StarStreamSource.getStarStreamSessionId();
      for (int seqId : missingChunkSeqIds) {
        if (addToIssuedChunkRequests(seqId)) {
          issueChunkRequest(sessionId, nextChunkSeqId);
        }
      }
    }
  }

  private void addToDeliveredChunks(int sequenceId) {
    deliveredChunks.add(sequenceId);
  }

  private boolean addToIssuedChunkRequests(int nextChunkSeqId) {
    return issuedChunkRequests.add(nextChunkSeqId);
  }

  private void checkIfPlaybackIsAllowed() {
    int contiguousChunks = getStore().countContiguousChunksFromStart(StarStreamSource.getStarStreamSessionId());
    if (contiguousChunks >= MIN_CONTIGUOUS_CHUNKS_IN_BUFFER) {
      startPalyBack();
    }
  }

  private List<Integer> collectMissingChunkIds() {
    List<Integer> seqIds = getStore().getMissingSequenceIds(StarStreamSource.getStarStreamSessionId());
    if (seqIds.size() == 0) {
      if (aggressive) {
        // we are really unlucky, the buffer is completely empty, but luckly we know
        // we can search for chunks with seqIDs from 0 to whatever we think is appropriate
        for (int i = 0; i < MIN_CONTIGUOUS_CHUNKS_IN_BUFFER; i++) {
          seqIds.add(i);
        }
      } else {
        seqIds.add(ChunkUtils.getMinSeqNumber());
      }
    }
    return seqIds;
  }

  boolean hasBeenDelivered(int seqId) {
    return deliveredChunks.contains(seqId);
  }

  /**
   * This method has to be invoked both at construction and cloning-time to let
   * the {@link StarStreamProtocol} instance register over the {@link PastryProtocol}
   * instance for Pastry-related events.
   */
  private void init() {
    getStarStreamProtocol().setOwner(this);
    getStarStreamProtocol().registerStarStreamListener(this);
    PastryProtocol pastry = getPastryProtocol();
    getStarStreamProtocol().registerPastryListeners(pastry);
    this.lastForce = 0;
    this.player = new StarStreamPlayer(this, StarStreamSource.getStarStreamSessionId(), chunkPlaybackLength, totalChunks);
    this.perceivedChunkDeliveryTimes = new IncrementalStats();
    this.issuedChunkRequests = new LinkedHashSet<Integer>();
    this.deliveredChunks = new TreeSet<Integer>();
    this.chunkRequestsForSeqIdsWithoutPastryIdYet = new LinkedList<Integer>();
  }

  private void removeFromIssuedChunkRequests(int sequenceId) {
    issuedChunkRequests.remove(sequenceId);
  }

  private void startPalyBack() {
    player.start();
    System.out.println("[MOJO] "+ this.getID() +" StartUpDelay: "+ player.getWhenPlaybackStarted());
    
    log("[*-STREAM] node " + this.getPastryId() + " has started playback");
  }

  private void updateLocalStats(Chunk<?> chunk) {
    long deliveryTime = CommonState.getTime() - chunk.getTimeStamp();
    perceivedChunkDeliveryTimes.add(deliveryTime);
  }
}