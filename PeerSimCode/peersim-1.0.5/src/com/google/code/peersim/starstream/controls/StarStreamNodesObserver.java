/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.google.code.peersim.starstream.controls;

import com.google.code.peersim.pastry.protocol.PastryId;
import com.google.code.peersim.starstream.protocol.StarStreamNode;
import com.google.code.peersim.starstream.protocol.StarStreamPlayer;
import com.google.code.peersim.starstream.protocol.StarStreamStore;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.FileNameGenerator;
import peersim.util.IncrementalStats;

/**
 * Observer class in charge of printing to file the state of each
 * {@link StarStreamNode} found in the {@link Network} at simulation completion.
 * 
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public class StarStreamNodesObserver implements Control {

	/**
	 * Whether to log or not.
	 */
	public static final String DO_LOG = "doLog";
	/**
	 * The file name to log to.
	 */
	public static final String LOG_FILE = "log";
	private static final String SEPARATOR = ".";
	/**
	 * Whether to log or not.
	 */
	private boolean doLog;
	/**
	 * The file name to log to.
	 */
	private String logFile;
	/**
	 * The stream to log to.
	 */
	private PrintStream stream;

	/**
	 * Constructor.
	 * 
	 * @param prefix
	 *            The prefix
	 * @throws java.io.FileNotFoundException
	 *             Throw iff the log file cannot be created
	 */
	public StarStreamNodesObserver(String prefix) throws FileNotFoundException {
		super();
		doLog = Configuration.getBoolean(prefix + SEPARATOR + DO_LOG);
		if (doLog) {
			String currentDir = System.getProperty("user.dir");
			currentDir = currentDir + "\\ExperimentResults\\";
			// logFile = currentDir + CommonState.getNetworkSize() + "-" + (int)
			// (((float) CommonState.getHelping() / (float)
			// CommonState.getNetworkSize()) * 100) + ".log";
			logFile = currentDir + CommonState.getNetworkSize() + "-"
					+ CommonState.getFileHelping() + ".log";
			stream = new PrintStream(new FileOutputStream(logFile));
		}
	}

	/**
	 * Once the very last simulation cycle begins, this method collects
	 * information related to each {@link StarStreamNode}'s
	 * {@link StarStreamStore} instance and print it all to the configured log
	 * file.
	 * 
	 * @return {@link Boolean#TRUE}
	 */
	@Override
	public boolean execute() {
		boolean stop = false;
		if (doLog && (CommonState.getTime() == CommonState.getEndTime() - 1)) {
			dump();
		}
		return stop;
	}

	/**
	 * Dumps down to the log file.
	 */
	private void dump() {
		IncrementalStats stats = new IncrementalStats();

		System.err.print("Dumping *-Stream stats to file " + logFile + "... ");

		for (int i = 0; i < Network.size(); i++) {
			StarStreamNode n = (StarStreamNode) (Network.get(i));
			// System.out.println("PID "+n.getPastryPID());
			/*if (!n.isHelping()) {
				System.out.println("ID " + n.getID());
			}*/
		}

		int helping = CommonState.getHelping() <= 0 ? 0 : CommonState
				.getHelping();

		// total chunks
		log("Total chunks: " + StarStreamSource.getTotalChunks());
		// nodes x chunk
		log("Nodes x chunk: " + StarStreamSource.getNodesPerChunk());
		// total nodes
		int dim = Network.size();
		log("Total nodes: " + dim);

		// active nodes
		int activeNodes = 0;
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (node.isUp() && !node.isHelping())
				activeNodes++;
		}
		log("Active nodes: " + activeNodes);

		// playback started
		int nodesThatStartedPlayback = 0;
		List<PastryId> nodesThatDidNotStartedPlayback = new LinkedList<PastryId>();
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				if (node.hasStartedPlayback())
					nodesThatStartedPlayback++;
				else
					nodesThatDidNotStartedPlayback.add(node.getPastryId());
			}
		}
		log("Started playbacks: " + nodesThatStartedPlayback);
		log("Not started playbacks node-ids: " + nodesThatDidNotStartedPlayback);

		// start-streaming time window
		long lastPlaybackStart = 0;
		long firstPlaybackStart = Long.MAX_VALUE;
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				long time = node.getWhenPlaybackStarted();
				if (time > lastPlaybackStart)
					lastPlaybackStart = time;
				if (time < firstPlaybackStart)
					firstPlaybackStart = time;
			}
		}
		log("Playbacks time-window: "
				+ (lastPlaybackStart - firstPlaybackStart));

		// missing chunks distribution
		Map<Integer, Integer> chunksTmpMap = new HashMap<Integer, Integer>();
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				int missingChunks = node.countMissingChunks();
				Integer nodesCount = chunksTmpMap.get(missingChunks);
				if (nodesCount == null) {
					chunksTmpMap.put(missingChunks, 1);
				} else {
					chunksTmpMap.put(missingChunks, ++nodesCount);
				}
			}
		}
		log("Missing chunks distribution [missed-chunks/nodes]: "
				+ chunksTmpMap);
		chunksTmpMap.clear();

		// TTL-rejected chunks stats
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				Set<Integer> missed = node.getStore()
						.getRejectedChunksDueToExpiration();
				for (int id : missed) {
					Integer nodesCount = chunksTmpMap.get(id);
					if (nodesCount == null) {
						chunksTmpMap.put(id, 1);
					} else {
						chunksTmpMap.put(id, ++nodesCount);
					}
				}
			}
		}
		log("Rejected chunks for ttl expiration [chunk-id/nodes]: "
				+ chunksTmpMap.size() + " " + chunksTmpMap);
		chunksTmpMap.clear();

		// capacity-rejected chunks stats
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				Set<Integer> missed = node.getStore()
						.getRejectedChunksDueToCapacityLimit();
				for (int id : missed) {
					Integer nodesCount = chunksTmpMap.get(id);
					if (nodesCount == null) {
						chunksTmpMap.put(id, 1);
					} else {
						chunksTmpMap.put(id, ++nodesCount);
					}
				}
			}
		}
		log("Rejected chunks for capacity limit [chunk-id/nodes]: "
				+ chunksTmpMap.size() + " " + chunksTmpMap);
		chunksTmpMap.clear();

		// bandwidthUtil - ELIJAH reprezent yea!
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				stats.add(node.getStarStreamProtocol().bandwidthUtilUp[(int)node.getID()].getAverage());
			}
		}
		log("bandwidthUtilUp: " + stats.getAverage());
		stats.reset();
		
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				stats.add(node.getStarStreamProtocol().bandwidthUtilDown[(int)node.getID()].getAverage());
			}
		}
		log("bandwidthUtilDown: " + stats.getAverage());
		stats.reset();
		
		// chunks not sent due to max-retries count
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				stats.add(node.getUnsentChunkMsgsDueToTimeout());
			}
		}
		log("Avg # of unsent chunk for max-retries: " + stats.getAverage());
		log("Min # of unsent chunk for max-retries: " + stats.getMin());
		log("Max # of unsent chunk for max-retries: " + stats.getMax());
		stats.reset();

		// chunk requests not sent due to max-retries count
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				stats.add(node.getUnsentChunkReqDueToTimeout());
			}
		}
		log("Avg # of unsent chunk-reqs for max-retries: " + stats.getAverage());
		log("Min # of unsent chunk-reqs for max-retries: " + stats.getMin());
		log("Max # of unsent chunk-reqs for max-retries: " + stats.getMax());
		stats.reset();

		// chunk received by means of Pastry
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				int pChunks = node.getChunksReceivedFromPastry();
				int sChunks = node.getChunksReceivedFromStarStream();
				double res = 0;
				if (pChunks + sChunks > 0)
					res = pChunks * 100 / (pChunks + sChunks);
				stats.add(res);
			}
		}
		log("Avg % of chunks received by Pastry: " + stats.getAverage());
		log("Min % of chunks received by Pastry: " + stats.getMin());
		log("Max % of chunks received by Pastry: " + stats.getMax());
		log("StD of chunks received by Pastry: " + stats.getStD());
		log("Var of chunks received by Pastry: " + stats.getVar());
		stats.reset();

		// chunk received by means of StarStream
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				int pChunks = node.getChunksReceivedFromPastry();
				int sChunks = node.getChunksReceivedFromStarStream();
				double res = 0;
				if (pChunks + sChunks > 0)
					res = sChunks * 100 / (pChunks + sChunks);
				stats.add(res);
			}
		}
		log("Avg % of chunks received by StarStream: " + stats.getAverage());
		log("Min % of chunks received by StarStream: " + stats.getMin());
		log("Max % of chunks received by StarStream: " + stats.getMax());
		log("StD of chunks received by StarStream: " + stats.getStD());
		log("Var of chunks received by StarStream: " + stats.getVar());
		stats.reset();

		// stats of perceived chunk delivery times
		double avgpb = 0;
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			// log("[MOJO] playback started:" +
			// node.getWhenPlaybackStarted());
			if (!node.isHelping()) {
				avgpb += node.getWhenPlaybackStarted();
				stats.add(node.getPerceivedAvgChunkDeliveryTime());
			}
		}
		log("[MOJO] Startup Delay: " + avgpb / (dim - helping));
		log("[MOJO] Latency: " + stats.getAverage());
		// log("Perceived avg chunk delivery-time: " + stats.getAverage());
		log("Min of perceived avg chunk delivery-time: " + stats.getMin());
		log("Max of perceived avg chunk delivery-time: " + stats.getMax());
		log("Variance of perceived avg chunk delivery-time: " + stats.getVar());
		log("StD of perceived avg chunk delivery-time: " + stats.getStD());

		// avg sent messages per node
		stats.reset();
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				stats.add(node.getSentMessages());
			}
		}
		log("Avg messages sent per node: " + stats.getAverage());
		log("Min messages sent per node: " + stats.getMin());
		log("Max messages sent per node: " + stats.getMax());
		log("Variance of messages sent per node: " + stats.getVar());
		log("StD of messages sent per node: " + stats.getStD());
		stats.reset();

		// players statistics
		int nodesWithUncompletePlaybacks = 0;
		/*
		 * if (CommonState.getTime() >= ((StarStreamNode)
		 * Network.get(0)).getStarStreamProtocol().getTimeIn() ||
		 * CommonState.getTime() <= (((StarStreamNode)
		 * Network.get(0)).getStarStreamProtocol().getTimeIn() +
		 * ((StarStreamNode)
		 * Network.get(0)).getStarStreamProtocol().getTimeStay())) { helping =
		 * CommonState.getHelping(); }
		 */
		System.err.println("\nHELPING:" + CommonState.getHelping());
		System.err.println("DIM:" + dim);
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				List<Integer> missed = node.getUnplayedChunks();
				if (missed.size() > 0)
					nodesWithUncompletePlaybacks++;
				stats.add(node.getPercentageOfUnplayedChunks());
				for (int id : missed) {
					Integer nodesCount = chunksTmpMap.get(id);
					if (nodesCount == null) {
						chunksTmpMap.put(id, 1);
					} else {
						chunksTmpMap.put(id, ++nodesCount);
					}
				}
			}
		}

		log("Nodes with incomplete playbacks: " + nodesWithUncompletePlaybacks);
		log("[MOJO] Packet Loss: " + stats.getAverage());
		// log("Avg % of not played chunks: " + stats.getAverage());
		log("Min % of not played chunks: " + stats.getMin());
		log("Max % of not played chunks: " + stats.getMax());
		log("Not played chunks [chunk-id/nodes]: " + chunksTmpMap);
		chunksTmpMap.clear();
		stats.reset();

		// distances between not played chunks
		IncrementalStats _stats = new IncrementalStats();
		for (int i = 0; i < dim; i++) {
			StarStreamNode node = (StarStreamNode) Network.get(i);
			if (!node.isHelping()) {
				List<Integer> missed = node.getUnplayedChunks();
				for (int j = missed.size() - 1; j > 0; j--) {
					_stats.add(missed.get(j) - missed.get(j - 1));
				}
				double avg = 0;
				if (_stats.getN() > 0) {
					avg = _stats.getAverage();
					if (avg != 0)
						stats.add(avg);
				}
				_stats.reset();
			}
		}
		log("Avg distance between not played chunks: " + stats.getAverage());
		log("Min distance between not played chunks: " + stats.getMin());
		log("Max distance between not played chunks: " + stats.getMax());
		log("");
		stats.reset();

		// players detail
		/*
		 * for (int i = 0; i < dim; i++) { StarStreamNode node =
		 * (StarStreamNode) Network.get(i); log(node.getPlayer().toString()); }
		 */
		System.err.print("done!\n\n");
	}

	/**
	 * Logging method.
	 * 
	 * @param msg
	 */
	private void log(String msg) {
		stream.println(msg);
	}
}
