/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.controls;

import java.util.UUID;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;

/**
 *
 * @author frusso
 * @version 0.1
 * @since 0.
 */
public class NodeCrasher implements Control {

  public static final String CRASH_WITH_CHUNK = "crashWithChunk";
  private int crashWithChunk;
  public static final String PERCENTAGE_OF_CRASHED_NODES = "percentageOfCrashedNodes";
  private float percentageOfCrashedNodes;
  private boolean taskAccomplished = false;

  public NodeCrasher(String prefix) {
    crashWithChunk = Configuration.getInt(prefix+"."+CRASH_WITH_CHUNK);
    percentageOfCrashedNodes = Configuration.getInt(prefix+"."+PERCENTAGE_OF_CRASHED_NODES);
  }

  @Override
  public boolean execute() {
    boolean stop = false;
    UUID sessionID = StarStreamSource.getStarStreamSessionId();
    int latestChunkSeqId = ChunkUtils.getLastGeneratedChunkSeqId(sessionID);
    if(latestChunkSeqId == crashWithChunk) {
      crashNodes();
    }
    return stop;
  }

  private void crashNodes() {
    if(!taskAccomplished) {
      float nodesToCrash = Network.size() * percentageOfCrashedNodes / 100;
      for(int i=0; i<nodesToCrash; i++) {
        int nodeToCrash = CommonState.r.nextInt(Network.size());
        Network.remove(nodeToCrash);
      }
      taskAccomplished = true;
    }
  }
}