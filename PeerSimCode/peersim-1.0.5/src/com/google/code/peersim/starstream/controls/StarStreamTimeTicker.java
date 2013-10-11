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
import peersim.util.IncrementalStats;

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
	public StarStreamTimeTicker(String prefix) {
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
		int size = CommonState.getNetworkSize();

		for (int i = 0; i < size; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			node.tick();
		}

		// [MOJO]
		StarStreamNode n = (StarStreamNode) Network.get(0);
		int numHelping = CommonState.getHelping();
		int numJoining = CommonState.getJoining();
		
		// ABSOLUTE CONTRIBUTION
		/*if (CommonState.getAbsConDown() == 0 && CommonState.getAbsConUp() == 0){
			IncrementalStats absConDown = new IncrementalStats();
			IncrementalStats absConUp = new IncrementalStats();
			for (int i = 0; i < size; i++){
				n = (StarStreamNode) Network.get(i);
				absConDown.add(n.getStarStreamProtocol().getDownStream());
				absConUp.add(n.getStarStreamProtocol().getUpStream());
			}
			
		}*/

		// JOINING PEERS
		if (CommonState.getTime() == CommonState.getTimeJoin()) {
			for (int i = 0; i < numJoining; i++) {
				n = (StarStreamNode) (Network.get(i)).clone();
				n.changeJoining(true);

				Object[] inits = Configuration
						.getInstanceArray(EDSimulator.PAR_INIT);
				for (int o = 0; o < inits.length; o++)
					((NodeInitializer) inits[o]).initialize(n);

				Network.add(n);
			}
		}

		// HELPING PEERS
		if (CommonState.getTime() == CommonState.getTimeIn()) {
			for (int i = 0; i < Math.abs(numHelping); i++) {
				if (numHelping < 0 || CommonState.isTwoWay()) {
					StarStreamNode node = (StarStreamNode) Network.get(i);
					node.getStarStreamProtocol().RemoveStreams();
				} 
				
				if (numHelping > 0 || CommonState.isTwoWay()) {
					n = (StarStreamNode) (Network.get(i)).clone();
					n.changeHelping(true);

					Object[] inits = Configuration
							.getInstanceArray(EDSimulator.PAR_INIT);
					for (int o = 0; o < inits.length; o++)
						((NodeInitializer) inits[o]).initialize(n);

					Network.add(n);
				}
			}

			if (numHelping > 0 || CommonState.isTwoWay()) {
				for (int i = 0; i < Network.size(); i++) {
					n = (StarStreamNode) (Network.get(i));
					if (!n.isHelping()) {
						n.getStarStreamProtocol().AddStreams();
					}
				}
			}
		}

		/*
		 * if (CommonState.getTime() == (CommonState.getTimeIn() +
		 * CommonState.getTimeStay())) { for (int i = 0; i <
		 * Math.abs(numHelping); i++) { if (numHelping <= 0) { StarStreamNode
		 * node = (StarStreamNode) Network.get(i);
		 * node.getStarStreamProtocol().AddStreams(); } else { n =
		 * (StarStreamNode) Network.remove();
		 * n.getStarStreamProtocol().RemoveStreams(); } } }
		 */

		// LEAVING PEERS
		if (CommonState.counter == 10) {
			// REMOVE FROM NETWORK
			for (int i = size - 1; i >= 0; i--) {
				StarStreamNode node = (StarStreamNode) Network.get(i);
				if (node.isJoining()) {
					Network.remove(i);
				}
			}
		}

		CommonState.setNetworkSize(Network.size());
		//CommonState.setChunkTTL(CommonState.getNetworkSize());

		return stop;
	}
}