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

package peersim;

import java.io.*;

import peersim.cdsim.*;
import peersim.config.*;
import peersim.core.*;
import peersim.edsim.*;
import peersim.util.IncrementalStats;

/**
 * This is the main entry point to peersim. This class loads configuration and
 * detects the simulation type. According to this, it invokes the appropriate
 * simulator. The known simulators at this moment, along with the way to detect
 * them are the following:
 * <ul>
 * <li>{@link CDSimulator}: if {@link CDSimulator#isConfigurationCycleDriven}
 * returns true</li>
 * <li>{@link EDSimulator}: if {@link EDSimulator#isConfigurationEventDriven}
 * returns true</li>
 * </ul>
 * This list represents the order in which these alternatives are checked. That
 * is, if more than one return true, then the first will be taken. Note that
 * this class checks only for these clues and does not check if the
 * configuration is consistent or valid.
 * 
 * @see #main
 */
public class Simulator {

	// ========================== static constants ==========================
	// ======================================================================

	/** {@link CDSimulator} */
	public static final int CDSIM = 0;

	/** {@link EDSimulator} */
	public static final int EDSIM = 1;

	/** Unknown simulator */
	public static final int UNKNOWN = -1;

	/** the class names of simulators used */
	protected static final String[] simName = { "peersim.cdsim.CDSimulator", "peersim.edsim.EDSimulator", };

	/**
	 * Parameter representing the number of times the experiment is run.
	 * Defaults to 1.
	 * 
	 * @config
	 */
	public static final String PAR_EXPS = "simulation.experiments";

	/**
	 * If present, this parameter activates the redirection of the standard
	 * output to a given PrintStream. This comes useful for processing the
	 * output of the simulation from within the simulator.
	 * 
	 * @config
	 */
	public static final String PAR_REDIRECT = "simulation.stdout";

	// [MOJO]
	public static final String PAR_NETSIZE = "network.size";
	public static final String PAR_CHUNKTTL = "network.chunkttl";
	public static final String PAR_SIZEINT = "network.sizeint";
	public static final String PAR_COLLABINT = "network.collabint";
	public static final String PAR_COLLABEXPS = "simulation.collabexps";
	public static final String PAR_HELPING = "protocol.mojocollab.helpingPeers";
	public static final String PAR_JOINING = "protocol.mojocollab.joiningPeers";
	public static final String PAR_TIMEIN = "protocol.mojocollab.timeIn";
	public static final String PAR_TIMESTAY = "protocol.mojocollab.timeStay";
	public static final String PAR_TIMEJOIN = "protocol.mojocollab.timeJoin";
	public static final String PAR_TWOWAY = "protocol.mojocollab.twoWay";

	// ==================== static fields ===================================
	// ======================================================================

	/** */
	private static int simID = UNKNOWN;

	// ========================== methods ===================================
	// ======================================================================

	/**
	 * Returns the numeric id of the simulator to invoke. At the moment this can
	 * be {@link #CDSIM}, {@link #EDSIM} or {@link #UNKNOWN}.
	 */
	public static int getSimID() {

		if (simID == UNKNOWN) {
			if (CDSimulator.isConfigurationCycleDriven()) {
				simID = CDSIM;
			} else if (EDSimulator.isConfigurationEventDriven()) {
				simID = EDSIM;
			}
		}
		return simID;
	}

	// ----------------------------------------------------------------------

	/**
	 * Loads the configuration and executes the experiments. The number of
	 * independent experiments is given by config parameter {@value #PAR_EXPS}.
	 * In all experiments the configuration is the same, only the random seed is
	 * not re-initialized between experiments.
	 * <p>
	 * Loading the configuration is currently done with the help of constructing
	 * an instance of {@link ParsedProperties} using the constructor
	 * {@link ParsedProperties#ParsedProperties(String[])}. The parameter
	 * <code>args</code> is simply passed to this class. This class is then used
	 * to initialize the configuration.
	 * <p>
	 * After loading the configuration, the experiments are run by invoking the
	 * appropriate engine, which is identified as follows:
	 * <ul>
	 * <li>{@link CDSimulator}: if
	 * {@link CDSimulator#isConfigurationCycleDriven} returns true</li>
	 * <li>{@link EDSimulator}: if
	 * {@link EDSimulator#isConfigurationEventDriven} returns true</li>
	 * </ul>
	 * <p>
	 * This list represents the order in which these alternatives are checked.
	 * That is, if more than one return true, then the first will be taken. Note
	 * that this class checks only for these clues and does not check if the
	 * configuration is consistent or valid.
	 * 
	 * @param args
	 *            passed on to
	 *            {@link ParsedProperties#ParsedProperties(String[])}
	 * @see ParsedProperties
	 * @see Configuration
	 * @see CDSimulator
	 * @see EDSimulator
	 */
	public static void main(String[] args) {
		long time = System.currentTimeMillis();

		System.err.println("Simulator: loading configuration");
		Configuration.setConfig(new ParsedProperties(args));

		PrintStream newout = (PrintStream) Configuration.getInstance(PAR_REDIRECT, System.out);
		if (newout != System.out)
			System.setOut(newout);

		int exps = Configuration.getInt(PAR_EXPS, 1);
		// [MOJO]
		int netsize = Configuration.getInt(PAR_NETSIZE, 1);
		int chunkttl = Configuration.getInt(PAR_CHUNKTTL, 1);
		int sizeint = Configuration.getInt(PAR_SIZEINT, 1);
		double collabint = Configuration.getDouble(PAR_COLLABINT, 1);
		int collabexps = Configuration.getInt(PAR_COLLABEXPS, 1);
		int helping = Configuration.getInt(PAR_HELPING, 1);
		int joining = Configuration.getInt(PAR_JOINING, 1);
		int timeIn = Configuration.getInt(PAR_TIMEIN, 1);
		int timeStay = Configuration.getInt(PAR_TIMESTAY, 1);
		int timeJoin = Configuration.getInt(PAR_TIMEJOIN, 1);
		boolean twoWay = Configuration.getBoolean(PAR_TWOWAY);

		CommonState.setOrigNetworkSize(netsize);
		CommonState.setNetworkSize(netsize);
		CommonState.setChunkTTL(chunkttl);
		CommonState.setHelping(helping);
		CommonState.setFileHelping(helping);
		CommonState.setJoining(joining);
		CommonState.setTimeIn(timeIn);
		CommonState.setTimeStay(timeStay);
		CommonState.setTimeJoin(timeJoin);
		CommonState.setTwoWay(twoWay);
		
		CommonState.startUp = new int[CommonState.getJoining()];
		
		int numHelping = Math.abs((int)((helping / 100.0) * CommonState.getOrigNetworkSize()));
		//System.out.println(Math.abs((int) (helping / 100.0)));
		CommonState.bandwidthUtilDown = new IncrementalStats[netsize + numHelping + 10];
		CommonState.bandwidthUtilUp = new IncrementalStats[netsize + numHelping + 10];
		for (int i = 0; i < netsize + numHelping + 10; i++) {
			CommonState.bandwidthUtilDown[i] = new IncrementalStats();
			CommonState.bandwidthUtilUp[i] = new IncrementalStats();
		}

		CommonState.downStreams = new int[netsize + numHelping + 10];
		CommonState.upStreams = new int[netsize + numHelping + 10];

		final int SIMID = getSimID();
		if (SIMID == UNKNOWN) {
			System.err.println("Simulator: unable to determine simulation engine type");
			return;
		}

		try {
			System.err.println("Number of experiments: " + exps);
			for (int k = 0; k < exps; ++k) {
				CommonState.setNetworkSize(netsize + (sizeint * k));
				CommonState.setChunkTTL(chunkttl + (sizeint * k));
				if (k > 0) {
					long seed = CommonState.r.nextLong();
					CommonState.initializeRandom(seed);
				}
				System.err.print("Simulator: starting experiment " + k);
				CommonState.setExpNum(k);
				System.err.println(" invoking " + simName[SIMID]);
				System.err.println("Random seed: " + CommonState.r.getLastSeed());
				System.out.println("\n\n");

				// XXX could be done through reflection, but
				// this is easier to read.
				// [MOJO]
				for (int j = 0; j < collabexps + 1; j++) {
					CommonState.setHelping((((float) helping / 100) * CommonState.getNetworkSize()) + (j * (collabint * CommonState.getNetworkSize())));
					if (CommonState.getHelping() > 0) {
						CommonState.setChunkTTL(CommonState.getChunkTTL() + CommonState.getHelping());
					}
					switch (SIMID) {
					case CDSIM:
						CDSimulator.nextExperiment();
						break;
					case EDSIM:
						EDSimulator.nextExperiment();
						break;
					}
				}
			}

		} catch (MissingParameterException e) {
			System.err.println(e + "");
			System.exit(1);
		} catch (IllegalParameterException e) {
			System.err.println(e + "");
			System.exit(1);
		}

		// undocumented testing capabilities
		if (Configuration.contains("__t"))
			System.out.println(System.currentTimeMillis() - time);
		if (Configuration.contains("__x"))
			Network.test();

	}

}
