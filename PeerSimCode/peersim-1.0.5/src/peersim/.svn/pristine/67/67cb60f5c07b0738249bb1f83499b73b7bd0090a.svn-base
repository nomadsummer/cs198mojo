package peersim.ps.pubsub;

import peersim.ps.types.PeerAddress;

public class PubSubEvent {
	private PubSubEventType type;
	private PeerAddress src;
	private Object event;
	
//------------------------------------------------------------------------	
	public PubSubEvent(PubSubEventType type, PeerAddress src, Object event) {
		this.type = type;
		this.src = src;
		this.event = event;
	}
	
//------------------------------------------------------------------------	
	public PubSubEventType getType() {
		return this.type;
	}

//------------------------------------------------------------------------	
	public PeerAddress getSrc() {
		return this.src;
	}

//------------------------------------------------------------------------	
	public Object getEvent() {
		return this.event;
	}
}
