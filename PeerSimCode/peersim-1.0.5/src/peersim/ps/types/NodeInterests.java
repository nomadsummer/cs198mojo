package peersim.ps.types;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Vector;

public class NodeInterests {

	private HashMap<Topic, PeerAddress> interests = new HashMap<Topic, PeerAddress>();

	private HashMap<BigInteger, Vector<LeaderInfo>> interestsInfo = new HashMap<BigInteger, Vector<LeaderInfo>>();
	
	private Vector<Topic> asArelayNode = new Vector<Topic>();

	
//------------------------------------------------------------------------	
	public NodeInterests() {
		interestsInfo = new HashMap<BigInteger, Vector<LeaderInfo>>();
	}

//------------------------------------------------------------------------	
// TODO: A node may also want to modify its interest, write the appropriate methods
	public void setLeaders(PeerAddress self, Vector<NodeProfile> neighbors) {
		PeerAddress leader;
		HashMap<PeerAddress, NodeProfile> interestedNeighbors = new HashMap<PeerAddress, NodeProfile>();

		for (Topic t : interests.keySet()) {
			for (NodeProfile np : neighbors) {
				if (np.getNodeInterests() != null && np.getNodeInterests().getInterests().containsKey(t))
					interestedNeighbors.put(np.getAddress(), np);
			}
			
			leader = findLeaderParent(self, t.getTopicId(), interestedNeighbors);
			interests.put(t, leader);
		}
	}
	
//------------------------------------------------------------------------	
	public PeerAddress findLeaderParent(PeerAddress self, BigInteger topicId,
			HashMap<PeerAddress, NodeProfile> neighbors) {
		
			PeerAddress leader = new PeerAddress(self.getNode(), self.getId());
			Vector<LeaderInfo> invalidInfo = new Vector<LeaderInfo>();
			Vector<LeaderInfo> candidates = new Vector<LeaderInfo>();
			Vector<LeaderInfo> myFinalInfo = new Vector<LeaderInfo>();
			Vector<LeaderInfo> neighborInfo;
			Vector<LeaderInfo> connectedInfo;
			
			if (candidates.isEmpty())
				candidates.add(new LeaderInfo(self, self, self, 0));

			for (PeerAddress p: neighbors.keySet()) {
				if (neighbors.get(p).getNodeInterests().contains(new Topic(topicId)) && betterThan(p.getId(), leader.getId(), topicId)) {
					candidates.add(new LeaderInfo(p, self, self, 0));	// add your neighbors to candidates
					leader = p;
				}
			}
				
			for (PeerAddress p: neighbors.keySet()) { // add to candidates, what ever your neighbors propose, if it is a new proposal and you are not their parent
				neighborInfo = neighbors.get(p).getNodeInterests().getLeaderInfo(topicId);
				if (neighborInfo != null) {
					for (LeaderInfo l :neighborInfo) {
						if (!l.parent.equals(self) && !candidates.contains(new LeaderInfo(l.leader, l.directlyConnected, p, l.age)) && 
							!candidates.contains(new LeaderInfo(l.leader, self, p, l.age))) { // note that contains does not care for "p" when it checks for "contains"!
								LeaderInfo ll = new LeaderInfo(l.leader, l.directlyConnected, p, l.age + 1);
								candidates.add(ll);
						}
					}
				}				
			}
				
			// find the invalid entries
			for (LeaderInfo l : candidates) {
				// XXX
				if (l.age > 100) // maybe its better to limit the size of the interestInfo vector, so if the size is above a threshold, remove the old entries
					invalidInfo.add(l);
				else if (!l.leader.equals(self) && l.directlyConnected.equals(self) && !neighbors.containsKey(l.leader) && !invalidInfo.contains(l)) // if you were directly connected, but not connected any more
					invalidInfo.add(l);
				else if (neighbors.containsKey(l.directlyConnected) && !invalidInfo.contains(l)) { 						// if the node who was directly connected, says it's not
					connectedInfo = neighbors.get(l.directlyConnected).getNodeInterests().getLeaderInfo(topicId);
					if (connectedInfo!= null && !connectedInfo.contains(l))
						invalidInfo.add(l);
					else if (betterThan(l.leader.getId(), leader.getId(), topicId))
						leader = l.leader;
				} else if (betterThan(l.leader.getId(), leader.getId(), topicId))
					leader = l.leader;
			}
			
		for (LeaderInfo l : invalidInfo)
			candidates.remove(l);

		for (LeaderInfo l : candidates) {
			if (l.leader.equals(leader) && !myFinalInfo.contains(l))
				myFinalInfo.add(l);
		}

		interestsInfo.put(topicId, myFinalInfo);
		
		return leader;
	}
	
//------------------------------------------------------------------------	
	public Vector<LeaderInfo> getLeaderInfo(BigInteger topicId) {
		return interestsInfo.get(topicId);
	}
	
//------------------------------------------------------------------------	
	public HashMap<BigInteger, Vector<LeaderInfo>> getInterestsInfo(BigInteger topicId) {
		return interestsInfo;
	}
	
//------------------------------------------------------------------------	
	public boolean betterThan(BigInteger peer1, BigInteger peer2, BigInteger topicId) {
		boolean result = false;
		
		if ((peer1.compareTo(peer2) < 0 && peer1.compareTo(topicId) >= 0) ||
			(peer1.compareTo(peer2) < 0 && peer1.compareTo(topicId) < 0 && peer2.compareTo(topicId) < 0) ||
			(peer1.compareTo(peer2) > 0 && peer2.compareTo(topicId) < 0 && peer1.compareTo(topicId) >= 0))
			result = true;
				
		return result;
	}

//------------------------------------------------------------------------	
	public HashMap<Topic, PeerAddress> getInterests() {
		return interests;
	}

//------------------------------------------------------------------------	
	public void subscribe(PeerAddress nodeId, Integer topicId) {
		Topic key = new Topic(topicId.toString());

		if (!interests.containsKey(key))
			interests.put(new Topic(topicId.toString()), nodeId);
		
		if (!interestsInfo.containsKey(key.getTopicId()))
			interestsInfo.put(key.getTopicId(), new Vector<LeaderInfo>());
	}

//------------------------------------------------------------------------	
	public void unsubscribe(Integer topicId) {
		Topic key = new Topic(topicId.toString());
		
		if (interests.containsKey(key))
			interests.remove(key);
		
		if (interestsInfo.containsKey(key.getTopicId()))
				interestsInfo.remove(key.getTopicId());
	}

//------------------------------------------------------------------------	
	public void clearRelayInterests() {
		asArelayNode.clear();
	}

//------------------------------------------------------------------------	
	public void addRelayInterest(Integer topicId) {
		Topic key = new Topic(topicId.toString());

		if (!asArelayNode.contains(key))
			asArelayNode.add(new Topic(topicId.toString()));
	}

//------------------------------------------------------------------------	
	public Vector<Topic> getRelayInterests() {
		return asArelayNode;
	}

//------------------------------------------------------------------------	
	public boolean contains(Topic t) {
		boolean result = false;

		if (interestsInfo.containsKey(t.getTopicId()))
			result = true;

		return result;
	}

//------------------------------------------------------------------------	
	public PeerAddress getLeader(BigInteger topicId) {
		return interests.get(new Topic(topicId));
	}

//------------------------------------------------------------------------	
	public String toString() {
		String str = "";

		for (Topic t : interests.keySet())
			str += "{topic: " + t + interestsInfo.get(t.getTopicId()) + "}" + " ";
		str += "";

		return str;
	}
}