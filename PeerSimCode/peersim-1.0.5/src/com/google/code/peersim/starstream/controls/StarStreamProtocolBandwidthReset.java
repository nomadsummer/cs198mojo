/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.controls;

import com.google.code.peersim.starstream.protocol.StarStreamNode;
import peersim.core.Control;
import peersim.core.Network;

/**
 * This control class is used to reset to zero the levels of used outbound and
 * inbound bandwidth on every {@link StarStreamNode} at every simulated-time click.
 * Moreover this class instances signal every node to process potentially pending
 * delayed message.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamProtocolBandwidthReset implements Control {

  /**
   * Constructor.
   * @param prefix The PeerSim configuration prefix
   */
  public StarStreamProtocolBandwidthReset(String prefix) {
  }

  /**
   * Tells every {@link StarStreamProtocol} instance that there has been
   * a new simulated-time tick and that both the outbound and inbound bandwithes
   * can be reset to their original levels.<br>
   * Moreover signal every node to process potentially pending delayed message.
   */
  @Override
  public boolean execute() {
    boolean stop = false;
    int dim = Network.size();
    for(int i=0; i<dim; i++) {
      StarStreamNode node = (StarStreamNode) Network.get(i);
      node.resetUsedBandwidth();
      node.processDelayedMessages();
    }
    return stop;
  }
}