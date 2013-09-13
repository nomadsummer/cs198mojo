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
import peersim.vector.*;
import peersim.util.IncrementalStats;
import peersim.transport.Transport;
import peersim.edsim.*;
import java.util.*;

/**
 */
public class OSObserver implements Control {

    // /////////////////////////////////////////////////////////////////////
    // Constants
    // /////////////////////////////////////////////////////////////////////

    /**
     * Config parameter that determines the accuracy for standard deviation
     * before stopping the simulation. If not defined, a negative value is used
     * which makes sure the observer does not stop the simulation
     * 
     * @config
     */
    private static final String PAR_ACCURACY = "accuracy";

    /**
     * The protocol to operate on.
     * 
     * @config
     */
    private static final String PAR_PROT = "protocol";

    // /////////////////////////////////////////////////////////////////////
    // Fields
    // /////////////////////////////////////////////////////////////////////

    /**
     * The name of this observer in the configuration. Initialized by the
     * constructor parameter.
     */
    private final String name;

    /**
     * Accuracy for standard deviation used to stop the simulation; obtained
     * from config property {@link #PAR_ACCURACY}.
     */
    private final double accuracy;

    /** Protocol identifier; obtained from config property {@link #PAR_PROT}. */
    private final int pid;

    // /////////////////////////////////////////////////////////////////////
    // Constructor
    // /////////////////////////////////////////////////////////////////////

    /**
     * Creates a new observer reading configuration parameters.
     */
    public OSObserver(String name) {
        this.name = name;
        accuracy = Configuration.getDouble(name + "." + PAR_ACCURACY, -1);
        pid = Configuration.getPid(name + "." + PAR_PROT);
    }

    // /////////////////////////////////////////////////////////////////////
    // Methods
    // /////////////////////////////////////////////////////////////////////

    /**
     * Print statistics for an average aggregation computation. Statistics
     * printed are defined by {@link IncrementalStats#toString}. The current
     * timestamp is also printed as a first field.
     * 
     * @return if the standard deviation is less than the given
     *         {@value #PAR_ACCURACY}.
     */
    public boolean execute() {
        long time = peersim.core.CommonState.getTime();

        IncrementalStats is  = new IncrementalStats();

        for (int i = 0; i < Network.size(); i++) {

	    OverlayStreaming prot = (OverlayStreaming) Network.get(i).getProtocol(pid);
            is.add(prot.getOnline());
        }


        /* Final statistics */
	if (CommonState.getPhase() == CommonState.POST_SIMULATION) {
		System.out.println("finalOnlineNodes: " + is.getSum() + " - TotalArrivals " + Network.size());
		for (int j = 0; j < Network.size(); j++) {
			OverlayStreaming prot = (OverlayStreaming) Network.get(j).getProtocol(pid);
			if (prot.getOnline() == 1) {
				System.out.println("nodeID: " + Network.get(j).getID() +
						   "\tdelay: " + prot.getDelay() + 
						   "\tnumParents: " + prot.getParents() + 
						   "\tnumActStripes/numStripes: " + prot.getNumActStripes() + 
						   "/" + prot.getNumStripes() 
						   ); 
//						   + "\tactStripesId/stripesId: " + prot.getActStripesId() + 
//						   "/" + prot.getStripesId() + 
//						   "\tchildren/tmpChildren/actChildren: " + prot.getChildren() +
//						   "/" + prot.getTmpChildren() + "/" + prot.getNumActChildren() +
//						   "\tBw/usedBw: " + prot.getBw() +
//						   "/" + prot.getUsedBw());
			}
		}
		for (int j = 0; j < Network.size(); j++) {
			OverlayStreaming prot = (OverlayStreaming) Network.get(j).getProtocol(pid);
			if (prot.getObserver()) {
				Vector timeObserved = prot.getTimeObserved();
				Vector parentsObserved = prot.getParentsObserved();
				for (int k=0; k<timeObserved.size(); k++){
					System.out.println("ObservedTime: " + timeObserved.elementAt(k) +
							   ", ObservedParents: " + parentsObserved.elementAt(k));
				}
			}
		}
		return (true);
	}
	return (false);
    }
}
