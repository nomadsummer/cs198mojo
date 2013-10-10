/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import peersim.core.CommonState;

/**
 *
 * @author frusso
 * @version
 * @since
 */
public class StarStreamPlayer {
	private boolean playbackStarted = false;
	private long whenPlaybackStarted = -1;
	private int lastPlayedChunkSeqId = -1;
	private final int chunkPlaybackLength;
	private final int howManyChunks;
	private final StarStreamNode node;
	private final UUID sessionId;
	private final List<Integer> playedChunks = new LinkedList<Integer>();
	private final List<Integer> missedChunks = new LinkedList<Integer>();

  public StarStreamPlayer(StarStreamNode node, UUID sessionId, int chunkLength, int howManyChunks) {
    chunkPlaybackLength = chunkLength;
    this.node = node;
    this.sessionId = sessionId;
    this.howManyChunks = howManyChunks;
  }

  @Override
  public String toString() {
    return "Node: "+node.getPastryId()+"\nPlayed: "+playedChunks+"\nMissed: "+missedChunks;
  }
  
  public int getPBLength(){
	  return chunkPlaybackLength;
  }

  List<Integer> getMissedChunks() {
    return Collections.unmodifiableList(missedChunks);
  }

  List<Integer> getPlayedChunks() {
    return Collections.unmodifiableList(playedChunks);
  }

  long getWhenPlaybackStarted() {
    return whenPlaybackStarted;
  }

  boolean isStarted() {
    return playbackStarted;
  }

  void start() {
	// [MOJO]
    playbackStarted = true;
    whenPlaybackStarted = CommonState.getTime();
    lastPlayedChunkSeqId = 0;
    playedChunks.add(lastPlayedChunkSeqId);
    if(node.isJoining()){
    	CommonState.startUp[CommonState.counter] = (int) whenPlaybackStarted;
    	CommonState.counter++;
    }
  }

  void tick() {
    if(playbackStarted && lastPlayedChunkSeqId<howManyChunks-1) {
      long nextChunktime = whenPlaybackStarted + ((lastPlayedChunkSeqId*chunkPlaybackLength)+chunkPlaybackLength);
      if(CommonState.getTime()==nextChunktime) {
        int nextId = ++lastPlayedChunkSeqId;
        if(node.hasBeenDelivered(nextId)) {
          // great, the chunk is there!
          playChunk(nextId);
        } else {
          // damn, this is a streaming-delay!
          missedChunks.add(nextId);
        }
      }
    }
  }

  private void playChunk(int seqId) {
    playedChunks.add(seqId);
  }
}