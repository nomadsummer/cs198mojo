package peersim.ps.types;

import java.math.BigInteger;


public class Topic {
	private BigInteger topicId;
	private int coverage = 0;
	private PeerAddress leader;

//------------------------------------------------------------------------
	public Topic(BigInteger id) {
		topicId = id;
	}
	
//------------------------------------------------------------------------
	public Topic(String id) {
		topicId = new BigInteger(id);
	}	
	
//------------------------------------------------------------------------
	public BigInteger getTopicId() {
		return topicId;
	}
	
//------------------------------------------------------------------------
	public int getCoveredBy() {
		return coverage;
	}
	
//------------------------------------------------------------------------
	public void increaseCoverage(int c) {
		coverage += c;
	}
	
//------------------------------------------------------------------------
	public void decreaseCoverage(int c) {
		coverage -= c;
	}
	
//------------------------------------------------------------------------
	public PeerAddress getLeader() {
		return leader;
	}
	
//------------------------------------------------------------------------
	public void setLeader(PeerAddress leader) {
		this.leader = leader;
	}
	
//------------------------------------------------------------------------
	public String toString() {
		return (topicId.toString());
	}

//------------------------------------------------------------------------
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		Topic other = (Topic) obj;
		if (topicId == null) {
			if (other.topicId != null)
				return false;
		} else if (!topicId.equals(other.topicId))
			return false;
		return true;
	}
}
