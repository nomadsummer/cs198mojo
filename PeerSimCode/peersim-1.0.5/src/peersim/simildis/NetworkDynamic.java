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

// package peersim.ps.controls;
package peersim.simildis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import peersim.config.Configuration;
import peersim.core.*;
import peersim.edsim.EDSimulator;
import peersim.edsim.NextCycleEvent;
import peersim.ps.newscast.Newscast;
import peersim.ps.pubsub.PubSub;
import peersim.ps.tman.TMan;
import peersim.ps.utility.FileIO;

public class NetworkDynamic implements Control {
	int round = -1;

	private static final String PAR_SUBST = "substitute";
	private static final String PAR_ADD = "add";
	private static final String PAR_MAX = "maxsize";
	private static final String PAR_MIN = "minsize";
	
	private static final String PAR_NEWSCAST_PROT="newscast";
	private static final String PAR_TMAN_PROT="tman";
	private static final String PAR_PUBSUB_PROT="pubsub";
	private static final String PAR_IDLENGTH = "idLength";
	private static final String PAR_K = "k";

	protected double add;
	protected boolean substitute;
	protected final int minsize;
	protected final int maxsize;
	
	private final int newscastPID;
	private final int tmanPID;
	private final int pubsubPID;
	private final int idLength;
	private final int k;
	
	private HashMap<Integer, Double> scenario = new HashMap<Integer, Double>();

//--------------------------------------------------------------------------
	public NetworkDynamic(String prefix) {
		this.add = Configuration.getDouble(prefix + "." + PAR_ADD);
		this.substitute = Configuration.getBoolean(prefix + "." + PAR_SUBST);
	
		this.maxsize = Configuration.getInt(prefix + "." + PAR_MAX, Integer.MAX_VALUE);
		this.minsize = Configuration.getInt(prefix + "." + PAR_MIN, 0);
		 
		this.newscastPID = Configuration.getPid(prefix + "." + PAR_NEWSCAST_PROT);
		this.tmanPID = Configuration.getPid(prefix + "." + PAR_TMAN_PROT);
		this.pubsubPID = Configuration.getPid(prefix + "." + PAR_PUBSUB_PROT);
		this.idLength = Configuration.getInt(prefix + "." + PAR_IDLENGTH);
		this.k = Configuration.getInt(prefix + "." + PAR_K);	
		
		loadscenario();
	}

//--------------------------------------------------------------------------	
	private void loadscenario() {
		
		Integer key;
		Double value;
		
		String []lineStr;
		String scenarioStr = FileIO.read("scenario");
		String []lines = scenarioStr.split("\n");
		for (int i= 0; i < lines.length; i++) {
			lineStr = lines[i].split("\t");
			key = Integer.parseInt(lineStr[0]);
			value = Double.parseDouble(lineStr[1]);
			if (scenario.containsKey(key))
				scenario.put(key, scenario.get(key) + value);
			else
				scenario.put(key, value);
		}
		
		System.out.println(scenario);
		
	}
//--------------------------------------------------------------------------
	public final boolean execute() {
		this.round++;
		
//		if (round % 2 != 0)
//			return false;

		Integer scenarioKey = new Integer(round);
		if (scenario.get(scenarioKey) != null)
			this.add = scenario.get(scenarioKey);
		else
			return false;
		
		int networkSize = Network.size();
		
		if (!this.substitute) {
			if ((this.maxsize <= networkSize && this.add > 0) || (this.minsize >= networkSize && this.add < 0))
				return false;
		}
		 
		int toadd = 0;
		int toremove = 0;
		if (this.add > 0) {
			toadd = (int)Math.round(this.add < 1 ? this.add * networkSize : this.add);
	
			if (!this.substitute && toadd > this.maxsize - networkSize)
				toadd = this.maxsize - networkSize;
			 
			if (this.substitute)
				toremove = toadd;
		} else if (this.add < 0) {
			toremove = (int)Math.round(this.add > -1 ? -this.add * networkSize : -this.add);
			 
			if (!this.substitute && toremove > networkSize - this.minsize)
				toremove = networkSize - this.minsize;
			 
			if (this.substitute)
				toadd = toremove;
		}
		 
		this.remove(toremove);
		this.add(toadd);
		 
		return false;
	}

//--------------------------------------------------------------------------
	protected void add(int n) {
		BigInteger newID;
		Node node;
		TMan tman;
		ArrayList<BigInteger> ids = new ArrayList<BigInteger>();
		
		for(int i = 0; i < Network.size(); i++) {			
			node = Network.get(i);
			tman = (TMan)node.getProtocol(this.tmanPID);
			ids.add(tman.getPeerAddress().getId());
		}
		
		final long time = CommonState.getTime();
		for (int i = 0; i < n; ++i) {
			Node newnode = (Node)Network.prototype.clone();
			newID = this.findUniqueID(ids);
			ids.add(newID);

			//XXX
			System.out.println("new node: " + newID);
			
			this.newscastInit(newnode, time);
			this.tmanInit(newnode, newID, time);
			this.pubsubInit(newnode, newID, time);

			Network.add(newnode);
		}
	}

//------------------------------------------------------------------
	protected void remove(int n) {
		for (int i = 0; i < n; ++i)
			Network.remove(CommonState.r.nextInt(Network.size()));
	}
	
//--------------------------------------------------------------------------
	private void newscastInit(Node node, long time) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Node> neighbors = new ArrayList<Node>();

		for (int i = 0; i < Network.size(); i++)
			nodes.add(Network.get(i));

		if (nodes.size() > this.k) {
			Collections.shuffle(nodes);
			neighbors.addAll(nodes.subList(0, this.k));
		} else
			neighbors = nodes;
			
		neighbors.remove(node);
		
		Newscast newscast = (Newscast)node.getProtocol(this.newscastPID);
				
		for (Node n : neighbors)
			newscast.addNeighbor(n);
		
		this.initProtocol(node, PAR_NEWSCAST_PROT, this.newscastPID, time);
	}

//--------------------------------------------------------------------------
	private void tmanInit(Node node, BigInteger id, long time) {
		TMan tman = (TMan)node.getProtocol(this.tmanPID);
		tman.init(node, id, this.idLength);
		this.initProtocol(node, PAR_TMAN_PROT, this.tmanPID, time);
	}

//--------------------------------------------------------------------------
	private void pubsubInit(Node node, BigInteger id, long time) {
		PubSub pubsub = (PubSub)node.getProtocol(this.pubsubPID);
		pubsub.init(node, id);
		this.initProtocol(node, PAR_PUBSUB_PROT, this.pubsubPID, time);
	}
	
//--------------------------------------------------------------------------
	private void initProtocol(Node node, String protocolName, int protocolID, long time) {
		Object protocolClone = null;
		NextCycleEvent protocolNCE = new NextCycleEvent(null);
		Scheduler scheduler = new Scheduler("protocol." + protocolName, false);
		
		try { 
			protocolClone = protocolNCE.clone(); 
		} catch(CloneNotSupportedException e) {} //cannot possibly happen
			
		final long delay = CommonState.r.nextLong(scheduler.step);
		final long nexttime = Math.max(time, scheduler.from) + delay;
		
		if (nexttime < scheduler.until )
			EDSimulator.add(nexttime - time, protocolClone, node, protocolID);
	}
	
//--------------------------------------------------------------------------
	private BigInteger findUniqueID(ArrayList<BigInteger> ids) {
		BigInteger id;
		
		while (true) {
			id = new BigInteger(this.idLength, CommonState.r);
			
			if (!ids.contains(id))
				break;				
		}
		
		return id;
	}

	@Override
	public boolean execute(int exp) {
		// TODO Auto-generated method stub
		return false;
	}
}