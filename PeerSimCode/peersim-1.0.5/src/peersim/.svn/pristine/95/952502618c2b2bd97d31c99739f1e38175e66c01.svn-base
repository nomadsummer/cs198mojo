package peersim.ps.types;

public class NodeUtility {
	private float utility;
	private NodeProfile np;	
	
//------------------------------------------------------------------------
	public NodeUtility(float utility, NodeProfile np) {
		this.setUtility(utility);
		this.np = np;
	}

//------------------------------------------------------------------------
	public void setUtility(float utility) {
		this.utility = utility;
	}

//------------------------------------------------------------------------
	public float getUtility() {
		return utility;
	}
	
//------------------------------------------------------------------------
	public NodeProfile getNodeProfile() {
		return np;
	}

//------------------------------------------------------------------------
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((np == null) ? 0 : np.hashCode());
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
		NodeUtility other = (NodeUtility) obj;
		if (np == null) {
			if (other.np != null)
				return false;
		} else if (!np.equals(other.np))
			return false;
		return true;
	}

}