package peersim.ps.pubsub;

import java.math.BigInteger;
import java.util.Set;
import java.util.Vector;

import peersim.cdsim.CDProtocol;
import peersim.core.*;
import peersim.config.Configuration;
import peersim.ps.tman.TMan;
import peersim.ps.types.Buckets;
import peersim.ps.types.NodeInterests;
import peersim.ps.types.NodeProfile;
import peersim.ps.types.PeerAddress;
import peersim.ps.types.Topic;
import peersim.ps.types.View;
import peersim.ps.utility.FileIO;
import peersim.transport.Transport;
import peersim.edsim.EDProtocol;

public class PubSub implements EDProtocol, CDProtocol {
	private static final String PAR_TMAN_PORT = "tman";
	private static final String PAR_TRANSPORT = "transport";
	private static final String PAR_TOPICS = "topics";
	private static final String PAR_BUCKETS = "buckets";
	private static final String PAR_SELECTEDBUCKETS = "selectedBuckets";
	private static final String PAR_SUBSCRIPTIONS = "subscriptions";
	private static final String PAR_CORRELATION = "correlation";
	
	private final String prefix;
    private final int tmanPID;
    private final int tid;
    
    private TMan tman;
	private PeerAddress self;
	private Transport transport;
	private int topics;
	private int buckets;
	private int selectedBuckets;
	private int subscriptions;
	private boolean correlation;

	private NodeInterests selfInterests;
	private NodeProfile selfProfile;
	private RelayPath relayPath;
	private View fans;
	private View friends;
	private Vector<NodeProfile> oldFriends;
	private Vector<TopicEvent> receivedEvents;
	
//------------------------------------------------------------------------
	public PubSub(String prefix) {
        this.prefix = prefix;    
		this.tmanPID = Configuration.getPid(prefix + "." + PAR_TMAN_PORT);
		this.tid = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
		this.topics = Configuration.getInt(prefix + "." + PAR_TOPICS);
		this.buckets = Configuration.getInt(prefix + "." + PAR_BUCKETS);
		this.selectedBuckets = Configuration.getInt(prefix + "." + PAR_SELECTEDBUCKETS);
		this.subscriptions = Configuration.getInt(prefix + "." + PAR_SUBSCRIPTIONS);
		this.correlation = Configuration.getBoolean(prefix + "." + PAR_CORRELATION);
		
		this.receivedEvents = new Vector<TopicEvent>();
		this.relayPath = new RelayPath();
		this.oldFriends = new Vector<NodeProfile>();
	}

//------------------------------------------------------------------------
	public Object clone() {
		PubSub pubsub = new PubSub(this.prefix);
		return pubsub;
	}
	
//------------------------------------------------------------------------
    public void init(Node node, BigInteger nodeId) {
    	this.self = new PeerAddress(node, nodeId);
    	this.tman = (TMan)node.getProtocol(this.tmanPID);
    	this.transport = ((Transport)node.getProtocol(tid));
    	this.subscribeToTopics();
    	this.selfProfile = new NodeProfile(this.self, this.selfInterests);
    	this.tman.initProfile(this.selfProfile);
    	this.friends = this.tman.getView();
    }

//------------------------------------------------------------------------
    public void processEvent(Node node, int protocolID, Object event) {
    	PubSubEvent pubsubEvent = (PubSubEvent)event;
    	
    	switch(pubsubEvent.getType()){
    	case PUBLISH:
    		this.handlePublish(pubsubEvent.getSrc(), (PublishEvent)pubsubEvent.getEvent(), protocolID);
    		break;
    	case TOPIC:
    		this.handleTopic(pubsubEvent.getSrc(), (TopicEvent)pubsubEvent.getEvent(), protocolID);
    		break;
    	}
    }
    
//------------------------------------------------------------------------
    public void handlePublish(PeerAddress src, PublishEvent event, int protocolID) {
		Set<Topic> topics = this.selfInterests.getInterests().keySet();
		
		if (topics == null || topics.isEmpty())
			return;
		
		BigInteger topicID = event.getTopicId();
		
		// XXX
		System.out.println(this.self.getId() + " publish topic id: " + topicID);		
		
		TopicEvent topicEvent = new TopicEvent(topicID, this.self, "");
		PubSubEvent newEvent = new PubSubEvent(PubSubEventType.TOPIC, this.self, topicEvent);
		
		this.receivedEvents.add(topicEvent);

		Vector<NodeProfile> interestedPeers = this.findInterestedPeers(this.self, topicID);

		for (NodeProfile np : interestedPeers) {
			this.transport.send(self.getNode(), np.getAddress().getNode(), newEvent, protocolID);
			System.out.println(this.self.getId() + " sends to " + np.getAddress().getId());
			String str = this.self.getId() + "->" + np.getAddress().getId() + ";\n";
			FileIO.append(str, "publish");
		}
    }
    
//------------------------------------------------------------------------
	public void handleTopic(PeerAddress src, TopicEvent event, int protocolID) {
		if (this.receivedEvents.contains(event))
			return;
		
		BigInteger topicID = event.getTopicId();
		
		// XXX
		System.out.println(this.self.getId() + " received topic id " + topicID + ", from: " + src.getId());		

		event.incrementHopCounts();
		this.receivedEvents.add(event);

		PubSubEvent newEvent = new PubSubEvent(PubSubEventType.TOPIC, this.self, new TopicEvent(event));
		
		Vector<NodeProfile> interestedPeers = this.findInterestedPeers(src, topicID);

		for (NodeProfile np : interestedPeers) {
			this.transport.send(this.self.getNode(), np.getAddress().getNode(), newEvent, protocolID);
			System.out.println(this.self.getId() + " sends to " + np.getAddress().getId());		
			String str = this.self.getId() + "->" + np.getAddress().getId() + ";\n";
			FileIO.append(str, "publish");
		}
	}

//------------------------------------------------------------------------
    public Vector<NodeProfile> findInterestedPeers(PeerAddress src, BigInteger topicID) {
		Vector<NodeProfile> interestedPeers = new Vector<NodeProfile>();

		// among your friends
		if (this.friends != null) {
			if (this.friends.getInterestedPeers(topicID) != null) 
				interestedPeers.addAll(this.friends.getInterestedPeers(topicID));
		
			if (this.friends.getInterestedRelayPeers(topicID) != null) {
				for (NodeProfile np : this.friends.getInterestedRelayPeers(topicID)) {
					if (!interestedPeers.contains(np))
						interestedPeers.add(np);
				}
			}
		}
		
		// or your fans
		if (this.fans != null) {
			for (NodeProfile np : this.fans.getInterestedPeers(topicID)) {
				if (!interestedPeers.contains(np))
					interestedPeers.add(np);
			}
			
			for (NodeProfile np : this.fans.getInterestedRelayPeers(topicID)) {
				if (!interestedPeers.contains(np))
					interestedPeers.add(np);
			}
		}
		
		// or on the relay path
		NodeProfile profile;
		if (this.relayPath.getRelayNeighbors(topicID) != null) {
			for (PeerAddress p : this.relayPath.getRelayNeighbors(topicID)) {
				if (p != null) {
					profile = new NodeProfile(p);
			
					if (!interestedPeers.contains(profile))
						interestedPeers.add(profile);
				}
			}
		}
		
		interestedPeers.remove(this.selfProfile);
		interestedPeers.remove(new NodeProfile(src));
		
		return interestedPeers;
    }

	
//------------------------------------------------------------------------
	private void subscribeToTopics() {

		Buckets b = new Buckets(this.topics, this.buckets);
		this.selfInterests = new NodeInterests();

		Vector<Integer> sampleTopics;
		if (this.correlation == true)
			sampleTopics = b.generateSample(this.subscriptions, this.selectedBuckets, 0.01); // generate, correlated, subscriptions
		else {
			Integer i;
			sampleTopics = new Vector<Integer>(); // generate uniform random subscriptions;
			
			while (sampleTopics.size() < this.subscriptions) {
				i = new Integer(CommonState.r.nextInt(this.topics) + 1);

				if (!sampleTopics.contains(i))
					sampleTopics.add(i);
			}
		}
		
		for (Integer d : sampleTopics)
			this.selfInterests.subscribe(this.self, d.intValue());
	}

//------------------------------------------------------------------------
	public void nextCycle(Node node, int protocolID) {
		// remove all those friends and fans with (p.isHeardFrom < 1)
		// be careful about those friend still reported by tman, but not
		// heard from during the last time interval
		// send your profile to all your alive friends (ProfileMessage)
		if (this.friends == null)
			this.friends = this.tman.getView();

		if (this.friends == null)
			return;


		// update your profile
		Vector<NodeProfile> neighbors = new Vector<NodeProfile>(this.friends.getNodes());
		
		if (this.fans != null) {
			for (NodeProfile np : this.fans.getNodes()) {
				if (!neighbors.contains(np))
					neighbors.add(np);
			}
		}
		
		this.selfInterests.setLeaders(this.self, neighbors);

		// if you are the leader, subscribe on the relay path, otherwise make sure you are not subscribed on any path
		BigInteger topicID;
		Set<Topic> topics = this.selfInterests.getInterests().keySet();
		PeerAddress nextHop, toUnsbscribe;

		neighbors.add(this.selfProfile);
		
		for (Topic t : topics) {
			topicID = t.getTopicId();
			
			if (this.self.equals(this.selfInterests.getLeader(topicID))) { // if you are the leader
				nextHop = this.findNextHop(topicID, neighbors);
				PubSub pubsub = (PubSub)nextHop.getNode().getProtocol(protocolID);
				
				if (!nextHop.equals(this.self)) { // and you are not the rendezvous point
					toUnsbscribe = this.relayPath.addRelayRequest(topicID, nextHop);
					pubsub.subscribeOnRelayPath(this.self, topicID, protocolID);
					
					if (toUnsbscribe != null && !toUnsbscribe.equals(nextHop)) {
						PubSub pubsubToUnsbscribe = (PubSub)toUnsbscribe.getNode().getProtocol(protocolID);
						pubsubToUnsbscribe.unsubscribeFromRelayPath(this.self, topicID, protocolID);					
					}
				} else if ((toUnsbscribe = this.relayPath.getReceiveFrom(topicID)) != null) { // but if you are the rendezvous point
					this.relayPath.removeRelayRequest(topicID);
					PubSub pubsubToUnsbscribe = (PubSub)toUnsbscribe.getNode().getProtocol(protocolID);
					pubsubToUnsbscribe.unsubscribeFromRelayPath(this.self, topicID, protocolID);
				}				
			} else if ((toUnsbscribe = this.relayPath.getReceiveFrom(topicID)) != null) { // but if you are not the leader
				this.relayPath.removeRelayRequest(t.getTopicId());
				PubSub pubsubToUnsbscribe = (PubSub)toUnsbscribe.getNode().getProtocol(protocolID);
				pubsubToUnsbscribe.unsubscribeFromRelayPath(this.self, topicID, protocolID);
			}
		}
		
		// Also refresh your relay path
		PeerAddress nextResponsibleNode;
		this.selfInterests.clearRelayInterests();
		
		for (BigInteger tID : this.relayPath.getRelayTopics()) {
			if (!this.selfInterests.contains(new Topic(tID))) {
				this.selfInterests.addRelayInterest(tID.intValue());
				nextResponsibleNode = findNextHop(tID, neighbors);
				PubSub pubsub = (PubSub)nextResponsibleNode.getNode().getProtocol(protocolID);
				
				// if you are not the rendezvous point and the next relay node is a new one
				if (!nextResponsibleNode.equals(this.self) && !nextResponsibleNode.equals(this.relayPath.getReceiveFrom(tID))) {
					toUnsbscribe = this.relayPath.addRelayRequest(tID, nextResponsibleNode);
					pubsub.subscribeOnRelayPath(this.self, tID, protocolID);
					
					if (toUnsbscribe != null && !toUnsbscribe.equals(nextResponsibleNode)) {
						PubSub pubsubToUnsbscribe = (PubSub)toUnsbscribe.getNode().getProtocol(protocolID);
						pubsubToUnsbscribe.unsubscribeFromRelayPath(this.self, tID, protocolID);					
					}					
				}				
			}
		}

		// send a "stop friendship" message to the old friends, who are not a friend anymore
		for (NodeProfile np : this.oldFriends) {
			if (!this.friends.contains(np)) {
				PubSub pubsub = (PubSub)np.getAddress().getNode().getProtocol(protocolID);
				pubsub.cancelFriendship(this.selfProfile, protocolID);
			}
		}
		
		this.oldFriends.clear();

		// send your profile to the alive friends
		for (NodeProfile np : this.friends.getNodes()) {
			PubSub pubsub = (PubSub)np.getAddress().getNode().getProtocol(protocolID);
			pubsub.refreshFriendship(this.selfProfile, protocolID);
			
			this.oldFriends.add(np);
		}
	}
	
	
//------------------------------------------------------------------------
	public boolean subscribeOnRelayPath(PeerAddress requester, BigInteger topicID, int protocolID) {
		boolean result;
		PeerAddress toUnsbscribe;
		this.relayPath.addRelayTo(topicID, requester);

		if (!this.selfInterests.contains(new Topic(topicID)) && this.relayPath.getReceiveFrom(topicID) == null) {
			Vector<NodeProfile> neighbors = new Vector<NodeProfile>();
			
			neighbors.addAll(this.getFriendsAndFans());				
			neighbors.add(new NodeProfile(this.self, this.selfInterests));

			PeerAddress nextHop = this.findNextHop(topicID, neighbors);
			PubSub pubsub = (PubSub)nextHop.getNode().getProtocol(protocolID);
			
			if (!nextHop.equals(this.self)) { // if you are not the rendezvous point
				toUnsbscribe = this.relayPath.addRelayRequest(topicID, nextHop);
				result = pubsub.subscribeOnRelayPath(this.self, topicID, protocolID);				
				
				if (toUnsbscribe != null && !toUnsbscribe.equals(nextHop)) {
						pubsub = (PubSub)toUnsbscribe.getNode().getProtocol(protocolID);
						pubsub.unsubscribeFromRelayPath(this.self, topicID, protocolID);						
				}
			} else // send a response back to the requester if you are the rendezvous point
				result = true;
		} else // if you are interested in this topic, or you are already on the path, send a response back
			result = true;
		
		return result;
	}
	
// -------------------------------------------------------------------
	public void unsubscribeFromRelayPath(PeerAddress requester, BigInteger topicID, int protocolID) {	
		PeerAddress toUnsbscribe = this.relayPath.removePath(topicID, requester, this.self);
		if (toUnsbscribe != null) {
			PubSub pubsub = (PubSub)toUnsbscribe.getNode().getProtocol(protocolID);
			pubsub.unsubscribeFromRelayPath(this.self, topicID, protocolID);
		}
	}
	
//------------------------------------------------------------------------
	private PeerAddress findNextHop(BigInteger topicId, Vector<NodeProfile> neighbors) {
		BigInteger currentNodeId, nextHopId;
		PeerAddress nextHop;

		nextHop = neighbors.elementAt(0).getAddress();
		nextHopId = nextHop.getId();

		for (NodeProfile np : neighbors) {
			currentNodeId = np.getAddress().getId();

			if ((currentNodeId.compareTo(nextHopId) < 0 && currentNodeId.compareTo(topicId) >= 0) ||
				(currentNodeId.compareTo(nextHopId) < 0 && currentNodeId.compareTo(topicId) < 0 && nextHopId.compareTo(topicId) < 0) ||
				(currentNodeId.compareTo(nextHopId) > 0 && nextHopId.compareTo(topicId) < 0 && currentNodeId.compareTo(topicId) >= 0)) {
				nextHop = np.getAddress();
				nextHopId = nextHop.getId();
			}
		}

		return nextHop;
	}
	
// -------------------------------------------------------------------
	public void refreshFriendship(NodeProfile requester, int protocolID) {
		if (this.fans == null)
			this.fans = new View(requester);
		
		else if (!this.fans.getNodes().contains(requester)) 
			this.fans.addNodeProfile(requester);
	}
	
//------------------------------------------------------------------------	
	public void cancelFriendship(NodeProfile requester, int protocolID) {
		if (this.fans != null && this.fans.getNodes().contains(requester))
			this.fans.removeNodeProfile(requester);	
	}
	
//------------------------------------------------------------------------	
	public NodeInterests getSelfInterests() {
		return this.selfInterests;
	}
	
//------------------------------------------------------------------------	
	public NodeProfile getNodeProfile() {
		return this.selfProfile;
	}
	
//------------------------------------------------------------------------	
	public RelayPath getRelayPath() {
		return this.relayPath;
	}
	
//------------------------------------------------------------------------	
	public View getFans() {
		return this.fans;
	}
	
//------------------------------------------------------------------------	
	public View getFriends() {
		return this.friends;
	}
	
//------------------------------------------------------------------------	
	private Vector<NodeProfile> getFriendsAndFans() {
		Vector<NodeProfile> neighbors = new Vector<NodeProfile>();
		
		if (this.friends != null)
				neighbors.addAll(this.friends.getNodes());

		if (this.fans != null) {
			for (NodeProfile np : this.fans.getNodes()) {
				if (!neighbors.contains(np))
					neighbors.add(np);
			}
		}
		
		return neighbors;
	}
}
