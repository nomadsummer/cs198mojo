/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.controls;

import com.google.code.peersim.starstream.protocol.StarStreamNode;
import peersim.core.Control;
import peersim.core.Network;

/**
 * Control class in charge of telling every active {@link StarStreamNode} to
 * check for messages whose timeout might have been expired.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamTimeTicker implements Control {

  /**
   * PeerSim dictated constructor.
   * @param prefix The configuration properties' prefix
   */
  public StarStreamTimeTicker(String prefix) {
    super();
  }

  /**
   * This method iterates over the full network of nodes, and if a node is up and
   * running ({@link StarStreamNode#isUp()}) invokes {@link StarStreamNode#checkForStarStreamTimeouts()}
   * over that node.
   * @return Whether the simulation must be halted or not
   */
  @Override
  public boolean execute() {
    boolean stop = false;
    int size = Network.size();
    for(int i=0; i<size; i++) {
      StarStreamNode node = (StarStreamNode) Network.get(i);
      node.tick();
    }
    return stop;
  }
}