package peersim.ps.types;

import java.math.BigInteger;
import java.util.Vector;

import peersim.core.CommonState;

public class View { 
	private final Vector<NodeProfile> nodes;

//------------------------------------------------------------------------		
	public View(Vector<NodeProfile> peers, int viewSize) {
		peers.removeElement(null);
		this.nodes = new Vector<NodeProfile>();
		for (NodeProfile p: peers) {
			if (this.nodes.size() <= viewSize)
				this.nodes.add(p);
			else
				break;
		}
	}
	
//------------------------------------------------------------------------		
	public View(NodeProfile peer) {
		this.nodes = new Vector<NodeProfile>();
		if (peer != null) 
			this.nodes.add(peer);
	}
	
//------------------------------------------------------------------------		
	public synchronized NodeProfile selectRandomPeer() {
		Integer index = CommonState.r.nextInt(this.nodes.size());
		NodeProfile randomPeer = this.nodes.get(index);
		
		return randomPeer;
	}

//------------------------------------------------------------------------		
	public synchronized int getSize() {
		return this.nodes.size();
	}

//------------------------------------------------------------------------		
	public synchronized void refresh(Vector<NodeProfile> selectedView) {
		this.nodes.clear();
		for (NodeProfile peer: selectedView) {
			if (peer != null && !this.nodes.contains(peer))
				this.nodes.add(peer);
		}
		
	}

//------------------------------------------------------------------------		
	public synchronized Vector<NodeProfile> getNodes() {
		return this.nodes;
	}

//------------------------------------------------------------------------		
	public synchronized void addNodeProfile(NodeProfile np) {
		if (np != null && !this.nodes.contains(np))
			this.nodes.add(np);
	}	

//------------------------------------------------------------------------		
	public synchronized void removeNodeProfile(NodeProfile np) {
		this.nodes.remove(np);
	}

//------------------------------------------------------------------------		
	public synchronized boolean contains(NodeProfile np) {
		return this.nodes.contains(np);
	}

//------------------------------------------------------------------------		
	public synchronized Vector<NodeProfile> getInterestedPeers(BigInteger topicId) {
		Vector<NodeProfile> interestedPeers = new Vector<NodeProfile>();
		
		for (NodeProfile np : this.nodes) {
			if (np.getNodeInterests() != null && np.getNodeInterests().getInterests().containsKey(new Topic(topicId)))
				interestedPeers.add(np);
		}

		return interestedPeers;
	}
	
//---------------------------------------------------------------------------------
	public synchronized Vector<NodeProfile> getInterestedRelayPeers(BigInteger topicId) {
		Vector<NodeProfile> interestedPeers = new Vector<NodeProfile>();

		for (NodeProfile np : this.nodes) {
			if (np.getNodeInterests() != null && np.getNodeInterests().getRelayInterests().contains(new Topic(topicId)))
				interestedPeers.add(np);
		}

		return interestedPeers;
	}

//------------------------------------------------------------------------		
	public synchronized PeerAddress getSucc(PeerAddress self) {
		PeerAddress succ;
		
		if (this.nodes.size() > 1)
			succ = this.nodes.elementAt(1).getAddress();
		else if (this.nodes.size() > 0)
			succ = self;
		else
			succ = self;
		
		return succ;	
	}

//------------------------------------------------------------------------		
	public synchronized PeerAddress getPred(PeerAddress self) {
		PeerAddress pred;			
		
		if (this.nodes.size() > 1)
			pred = this.nodes.elementAt(0).getAddress();
		else if (this.nodes.size() > 0)
			pred = this.nodes.elementAt(0).getAddress();
		else
			pred = self;			
		
		return pred;	
	}

//------------------------------------------------------------------------		
	public synchronized Vector<PeerAddress> getCluster() {
		Vector<PeerAddress> cluster = new Vector<PeerAddress>();
		
		if (this.nodes.size() > 3) {
			for (NodeProfile profile : this.nodes.subList(3, this.nodes.size()))
				cluster.addElement(profile.getAddress());
		}
		
		return cluster;	
	}
}
