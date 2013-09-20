/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.controls;

import java.io.FileNotFoundException;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;
import peersim.edsim.EDSimulator;

import com.google.code.peersim.starstream.protocol.StarStreamNode;
import com.google.code.peersim.starstream.protocol.StarStreamProtocol;

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
	 * 
	 * @param prefix
	 *            The configuration properties' prefix
	 */
	public StarStreamTimeTicker(String prefix){
		super();
		
	}

	/**
	 * This method iterates over the full network of nodes, and if a node is up
	 * and running ({@link StarStreamNode#isUp()}) invokes
	 * {@link StarStreamNode#checkForStarStreamTimeouts()} over that node.
	 * 
	 * @return Whether the simulation must be halted or not
	 */
	@Override
	public boolean execute() {
		boolean stop = false;
		int size = Network.size();

		for (int i = 0; i < size; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			node.tick();
		}

		// [MOJO]
		//if (CommonState.getTime() % 10 == 0)
			//System.err.println("Current time: " + CommonState.getTime() + " | Size: " + Network.size());
		
		StarStreamNode n = (StarStreamNode) Network.get(0);

		if (CommonState.getTime() == n.getStarStreamProtocol().getTimeIn()) {
			for (int i = 0; i < n.getStarStreamProtocol().numHelpingPeers(); i++) {
				n = (StarStreamNode) (Network.get(i)).clone();
				Network.add(n);
				n.getStarStreamProtocol().AddStreams();
				//n.getBandwidth();

				Object[] inits = Configuration.getInstanceArray(EDSimulator.PAR_INIT);
				for (int o = 0; o < inits.length; o++)
					((NodeInitializer) inits[o]).initialize(n);
				//System.err.println("Current time: " + CommonState.getTime() + " | Size: " + Network.size());
			}
		}

		if (CommonState.getTime() == (n.getStarStreamProtocol().getTimeIn() + n.getStarStreamProtocol().getTimeStay())) {
			for (int i = 0; i < n.getStarStreamProtocol().numHelpingPeers(); i++) {
				n = (StarStreamNode) Network.remove();
				n.getStarStreamProtocol().RemoveStreams();
				//n.getBandwidth();
				
				//System.err.println("Current time: " + CommonState.getTime() + " | Size: " + Network.size());
			}
		}

		return stop;
	}
}