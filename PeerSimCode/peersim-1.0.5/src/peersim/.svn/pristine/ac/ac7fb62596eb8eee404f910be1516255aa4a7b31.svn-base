package peersim.ps.types;

public class LeaderInfo {
	public PeerAddress leader;
	public PeerAddress directlyConnected;
	public PeerAddress parent;
	public int age;
	
//------------------------------------------------------------------------
	public LeaderInfo(PeerAddress leader, PeerAddress directlyConnected, PeerAddress parent, int age) {
		this.leader = leader;
		this.parent = parent;
		this.directlyConnected = directlyConnected;
		this.age = age;
	}

	
//------------------------------------------------------------------------
	public String toString() {
		return leader.getId() + ":" + directlyConnected + ":" + parent.getId() + "  age:" + age;
	}

//------------------------------------------------------------------------
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((directlyConnected == null) ? 0 : directlyConnected
						.hashCode());
		result = prime * result + ((leader == null) ? 0 : leader.hashCode());
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
		LeaderInfo other = (LeaderInfo) obj;
		if (directlyConnected == null) {
			if (other.directlyConnected != null)
				return false;
		} else if (!directlyConnected.equals(other.directlyConnected))
			return false;
		if (leader == null) {
			if (other.leader != null)
				return false;
		} else if (!leader.equals(other.leader))
			return false;
		return true;
	}
}