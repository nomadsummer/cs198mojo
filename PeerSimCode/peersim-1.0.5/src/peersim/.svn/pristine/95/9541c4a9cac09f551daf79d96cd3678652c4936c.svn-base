package peersim.ps.pubsub;

import java.math.BigInteger;

import peersim.ps.types.PeerAddress;

/**
 * The <code>TopicEvent</code> class represent an event which is published on a topic.
 * 
 * @author Fatemeh Rahimian	
 */

public final class TopicEvent implements Comparable<TopicEvent> {

	private final BigInteger topicId;
	private final long eventId;
	private final PeerAddress creator;
	private final String content;
	private int hopCounts = 0;
	
//------------------------------------------------------------------------
	public TopicEvent(BigInteger topicId, long eventId, PeerAddress creator, String content) {
		this.topicId = topicId;
		this.eventId = eventId;
		this.creator = creator;
		this.content = content;
	}

//------------------------------------------------------------------------
	public TopicEvent(BigInteger topicId, PeerAddress creator, String content) {
		this.topicId = topicId;
		this.eventId = System.currentTimeMillis();
		this.creator = creator;
		this.content = content;
	}

//------------------------------------------------------------------------
	public TopicEvent(TopicEvent topicEvent) {
		this.topicId = topicEvent.topicId;
		this.eventId = topicEvent.eventId;
		this.creator = topicEvent.creator;
		this.content = topicEvent.content;
		this.hopCounts = topicEvent.hopCounts;
	}	
	
//------------------------------------------------------------------------
	public BigInteger getTopicId() {
		return topicId;
	}
	
//------------------------------------------------------------------------
	public long getEventId() {
		return eventId;
	}
	
//------------------------------------------------------------------------
	public PeerAddress getCreator() {
		return creator;
	}
	
//------------------------------------------------------------------------
	public String getContent() {
		return content;
	}

//------------------------------------------------------------------------
	public void incrementHopCounts() {
		this.hopCounts++;
	}
	
//------------------------------------------------------------------------
	public int getHopCounts() {
		return hopCounts;
	}

//------------------------------------------------------------------------
	public String toString() {
		return "{@" + topicId.toString() + " from " + creator.toString() + "}"; 
	}

//------------------------------------------------------------------------
	@Override
	public int compareTo(TopicEvent o) {
		// TODO Auto-generated method stub
		if (this.equals(o))
			return 0;
		else
			return 1;
	}

//------------------------------------------------------------------------
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + ((creator == null) ? 0 : creator.hashCode());
		result = prime * result + (int) (eventId ^ (eventId >>> 32));
		result = prime * result + ((topicId == null) ? 0 : topicId.hashCode());
		return result;
	}

//------------------------------------------------------------------------
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TopicEvent other = (TopicEvent) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (creator == null) {
			if (other.creator != null)
				return false;
		} else if (!creator.equals(other.creator))
			return false;
		if (eventId != other.eventId)
			return false;
		if (topicId == null) {
			if (other.topicId != null)
				return false;
		} else if (!topicId.equals(other.topicId))
			return false;
		return true;
	}
}
