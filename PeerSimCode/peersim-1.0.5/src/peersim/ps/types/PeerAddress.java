package peersim.ps.types;

import java.math.BigInteger;

import peersim.core.Node;

public class PeerAddress implements Comparable<PeerAddress> {
	private BigInteger id;
	private Node node;
	
//------------------------------------------------------------------------	
	public PeerAddress(Node node, BigInteger id) {
		this.id = id;
		this.node = node;
	}
	
//------------------------------------------------------------------------	
	public Node getNode() {
		return this.node;
	}

//------------------------------------------------------------------------	
	public BigInteger getId() {
		return this.id;
	}

//------------------------------------------------------------------------
	@Override
	public String toString() {
		return "(id=" + id.intValue() + ", node=" + node.getID() + ")";
	}

//------------------------------------------------------------------------
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		PeerAddress other = (PeerAddress) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

//------------------------------------------------------------------------
	@Override
	public int compareTo(PeerAddress that) {
		return this.id.compareTo(that.id);
	}
}
