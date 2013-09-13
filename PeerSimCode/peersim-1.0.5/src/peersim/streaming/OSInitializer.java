/*
 * Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package peersim.streaming;

import peersim.config.*;
import peersim.core.*;
import peersim.transport.Transport;
import peersim.edsim.*;


/**
 */
public class OSInitializer implements Control {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    private static final String PAR_TOTALNODES    = "totalnodes";
    private static final String PAR_INITNODES     = "initnodes";
    private static final String PAR_NUMSTRIPES    = "numstripes";
    private static final String PAR_ACTIVESTRIPES = "activestripes";
    private static final String PAR_PCHURN        = "pchurn";

    /**
     * The protocol to operate on.
     * 
     * @config
     */
    private static final String PAR_PROT = "protocol";

    private static final double[] inputBandwidth = {0.1, 0.2, 0.5 };

//    private static final double[] inputProbab    = {0.2 , 0.4, 0.4};
//   you need the CDF
    private static final double[] inputProbab    = {0.2 , 0.6, 1.0};
    
    
    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    /** Protocol identifier; obtained from config property {@link #PAR_PROT}. */
    private final int pid;
    private final int totalnodes, initnodes, numstripes, activestripes;
    private final double pchurn;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Creates a new instance and read parameters from the config file.
     */
    public OSInitializer(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        totalnodes    = Configuration.getInt(prefix + "." + PAR_TOTALNODES, 1000);
        initnodes     = Configuration.getInt(prefix + "." + PAR_INITNODES, 100);
        numstripes    = Configuration.getInt(prefix + "." + PAR_NUMSTRIPES, 10);
        activestripes = Configuration.getInt(prefix + "." + PAR_ACTIVESTRIPES, 6);
        pchurn        = Configuration.getDouble(prefix + "." + PAR_PCHURN,   1.0);

    }

    // ------------------------------------------------------------------------
    // Methods
    // ------------------------------------------------------------------------

    /**
    * 
    */
    public boolean execute() {
    
	int index, i, j;
	int MAXVALUE = 1000; 
	
	/* the size of the network is NUM_STRIPES + 1 */
	for (i = 0; i < Network.size()-1; i++) {
		OverlayStreaming prot = (OverlayStreaming) Network.get(i).getProtocol(pid);
		prot.resetAll();
		/* set the bandwidth */
		index = CommonState.r.nextInt(MAXVALUE);
		prot.setBw(inputBandwidth[0]);
		for (j = 0; j < inputProbab.length-1; j++){
			if (index > MAXVALUE*inputProbab[j]){
				prot.setBw(inputBandwidth[j+1]);
			}//if
		}//for j
		/* set the stripe */
		prot.setParents(1);
		prot.setNumActStripes(1);
		prot.setActStripesId((int)Math.pow(2,i));
		prot.setDelay(0); //this should be set as the first chunk delay;
		prot.setParentValues(0, (int)Math.pow(2,i), null, 0);
        }
	/* now set the last node as the first arriving node */
	i = Network.size()-1;
	Node node = Network.get(i);
	OverlayStreaming prot = (OverlayStreaming) node.getProtocol(pid);
	prot.resetAll();
	
	prot.setTotalNodes(totalnodes);
	prot.setInitNodes(initnodes);
	prot.setGlobalStripes(numstripes);
	prot.setActiveStripes(activestripes);
	prot.setPChurn(pchurn);
	prot.setOtherParameters();
		System.out.println("Init" + ", node: " + node.getID() + ", numNode: " + Network.size() +
					" endTime: " + peersim.core.CommonState.getEndTime() +
					" Phase: " + CommonState.getPhase());
	if (CommonState.getPhase() == CommonState.POST_SIMULATION)
		CommonState.setPhase(CommonState.PHASE_UNKNOWN);
	EDSimulator.add(1, new ArrivedMessage(0, node, 0, 0), node, pid);
	/* the first '0' stands for the event type (EV_GENERATION) */

	return false;
    }
}
