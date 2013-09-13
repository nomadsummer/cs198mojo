package peersim.ps.controls;

import java.util.ArrayList;
import java.util.HashMap;

import peersim.core.*;
import peersim.config.Configuration;
import peersim.edsim.EDSimulator;
import peersim.ps.pubsub.PubSub;
import peersim.ps.pubsub.PubSubEvent;
import peersim.ps.pubsub.PubSubEventType;
import peersim.ps.pubsub.PublishEvent;
import peersim.ps.types.NodeProfile;
import peersim.ps.types.PeerAddress;
import peersim.ps.types.Topic;

public class NetworkScenario implements Control {
	private static final String PAR_PUBSUB_PROT="pubsub";
	private final int pubsubPID;
	private boolean hasStarted;
	
	
//------------------------------------------------------------------------	
	public NetworkScenario(String prefix) {
		this.hasStarted = false;
		this.pubsubPID = Configuration.getPid(prefix + "." + PAR_PUBSUB_PROT);
	}
	
//------------------------------------------------------------------------	
	public boolean execute() {
		if (CommonState.getTime() == 0)
			return false;
		
		if (!this.hasStarted) {
			this.hasStarted = true;
			
			NodeProfile nodeProfile;
			HashMap<Topic, ArrayList<PeerAddress>> topics = new HashMap<Topic, ArrayList<PeerAddress>>();
			
			
			for (int i = 0; i < Network.size(); i++) {
	        	nodeProfile = ((PubSub)Network.get(i).getProtocol(this.pubsubPID)).getNodeProfile();
	        	
	        	for (Topic topic : nodeProfile.getNodeInterests().getInterests().keySet()) {
	        		if (!topics.containsKey(topic))
	        			topics.put(topic, new ArrayList<PeerAddress>());
	        		
	        		topics.get(topic).add(nodeProfile.getAddress());
	        	}
	        }
			
			int count = 0;
			Node publisher;
			ArrayList<PeerAddress> interestedNodes;
			for (Topic topic : topics.keySet()) {
				interestedNodes = topics.get(topic);
				publisher = interestedNodes.get(CommonState.r.nextInt(interestedNodes.size())).getNode();
				
				PubSub pubsub = (PubSub)publisher.getProtocol(this.pubsubPID);
				PeerAddress publisherAddress = pubsub.getNodeProfile().getAddress(); 
				
				PublishEvent publishEvent = new PublishEvent(publisherAddress.getId(), topic.getTopicId());
				PubSubEvent event = new PubSubEvent(PubSubEventType.PUBLISH, publisherAddress, publishEvent);
				
				EDSimulator.add(count, event, publisher, this.pubsubPID);
				
				count += 100;
			}
		}
		
		return false;
	}

	@Override
	public boolean execute(int exp) {
		// TODO Auto-generated method stub
		return false;
	}
}
