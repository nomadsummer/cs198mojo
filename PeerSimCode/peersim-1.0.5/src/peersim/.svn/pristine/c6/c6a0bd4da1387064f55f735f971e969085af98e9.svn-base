package peersim.ps.types;

public class NodeProfile {

	private PeerAddress address;
	private NodeInterests interests = null;
	
//------------------------------------------------------------------------	
	public NodeProfile(NodeProfile np) {
		this.address = np.getAddress();
		this.interests = np.getNodeInterests();
	}

//------------------------------------------------------------------------	
	public NodeProfile(PeerAddress address, NodeInterests interests) {
		this.address = address;
		this.interests = interests;
	}

//------------------------------------------------------------------------	
	public NodeProfile(PeerAddress address) {
		this.address = address;
	}
	
//------------------------------------------------------------------------	
	public void setAddress(PeerAddress address) {
		this.address = address;
	}

//------------------------------------------------------------------------	
	public PeerAddress getAddress() {
		return address;
	}

//------------------------------------------------------------------------	
	public void setInterests(NodeInterests interests) {
		this.interests = interests;
	}

//------------------------------------------------------------------------	
	public NodeInterests getNodeInterests() {
		return interests;
	}


//------------------------------------------------------------------------	
	public String toString() {
//		String interestsStr;
//		
//		if (interests == null || interests.getInterests() == null)
//			interestsStr = "";
//		else
//			interestsStr = interests.getInterests().keySet().toString();
//		
//		String str = "[node: " + address.getId() + " => (" + this.interests + ")] ";
//
//		return str;
		
		return this.address.getId().toString();
	}

//------------------------------------------------------------------------	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
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
		NodeProfile other = (NodeProfile) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		return true;
	}
}