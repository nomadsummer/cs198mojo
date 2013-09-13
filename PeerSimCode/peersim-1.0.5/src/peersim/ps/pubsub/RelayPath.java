package peersim.ps.pubsub;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Vector;

import peersim.ps.types.PeerAddress;

public class RelayPath {
	private HashMap<BigInteger, Vector<PeerAddress>> relayTo;
	private HashMap<BigInteger, PeerAddress> receiveFrom;
	
//------------------------------------------------------------------------	
	public RelayPath() {
		relayTo = new HashMap<BigInteger, Vector<PeerAddress>>();
		receiveFrom = new HashMap<BigInteger, PeerAddress>();
	}

//------------------------------------------------------------------------	
	public void addRelayTo(BigInteger t, PeerAddress requester) {
		Vector<PeerAddress> peers;
	
		if (!relayTo.containsKey(t)) {
			peers = new Vector<PeerAddress>();
			peers.add(requester);
			relayTo.put(t, peers);				
		} else {
			peers = relayTo.get(t);
			if (!peers.contains(requester)) {
				peers.add(requester);
				relayTo.put(t, peers);
			}
		}
	}
	
//------------------------------------------------------------------------	
	public PeerAddress addRelayRequest(BigInteger t, PeerAddress nextHop) {
		PeerAddress toUnsubscribe = null;
		
		if (receiveFrom.containsKey(t) && !receiveFrom.get(t).equals(nextHop))
			toUnsubscribe = receiveFrom.get(t);
		
		receiveFrom.put(t, nextHop);

		return toUnsubscribe;
	}
	
//------------------------------------------------------------------------	
	public PeerAddress removeRelayRequest(BigInteger topicId) {
		PeerAddress toUnsubscribe = receiveFrom.get(topicId);
		receiveFrom.remove(topicId);
		
		return toUnsubscribe;		
	}	

//------------------------------------------------------------------------	
	public PeerAddress removePath(BigInteger t, PeerAddress requester, PeerAddress self) {
		PeerAddress toUnsubscribe = null;
		Vector<PeerAddress> tempList;
		if (relayTo.get(t) != null && relayTo.get(t).contains(requester)) {
			tempList = relayTo.get(t);
			tempList.remove(requester);
			if (!tempList.isEmpty())
				relayTo.put(t, tempList);
			else {
				relayTo.remove(t);
				toUnsubscribe = receiveFrom.get(t);
				receiveFrom.remove(t);
			}
		} else if (relayTo.get(t) == null) {
			toUnsubscribe = receiveFrom.get(t);
			receiveFrom.remove(t);
		}
				
		return toUnsubscribe;
	}
	
//------------------------------------------------------------------------	
	public Vector<PeerAddress> getRelayNeighbors(BigInteger t) {
		Vector<PeerAddress> peers = new Vector<PeerAddress>();
		
		if (relayTo.get(t) != null)
			peers.addAll(relayTo.get(t));
		
		if (receiveFrom.get(t) != null)
			peers.add(receiveFrom.get(t));

		return peers;
		
	}

//------------------------------------------------------------------------	
	public Vector<PeerAddress> getRelayTo(BigInteger topicId) {
		return relayTo.get(topicId);
	}	

//------------------------------------------------------------------------	
	public PeerAddress getReceiveFrom(BigInteger topicId) {
		return receiveFrom.get(topicId);
	}

//------------------------------------------------------------------------	
	public Vector<BigInteger> getRelayTopics() {
		Vector<BigInteger> relayTopics = new Vector<BigInteger>();
		relayTopics.addAll(relayTo.keySet());
		return relayTopics;
	}
	
//------------------------------------------------------------------------	
	public String toString(BigInteger topicId) {
		String str = "@" + topicId;
		str += " receives from: " + receiveFrom.get(topicId);
		str += "\t relays to: " + relayTo.get(topicId);

		return str;
	}
}