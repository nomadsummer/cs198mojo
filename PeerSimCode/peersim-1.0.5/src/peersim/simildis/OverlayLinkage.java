package peersim.simildis;

import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * A protocol that stores links. It does nothing apart from that.
 * It is useful to model a static link-structure
 * (topology). The only function of this protocol is to serve as a source of
 * neighborhood information for other protocols.
 */
public class OverlayLinkage implements Protocol, Linkable
{

// --------------------------------------------------------------------------
// Parameters
// --------------------------------------------------------------------------

/**
 * Default init capacity
 */
private static final int DEFAULT_INITIAL_CAPACITY = 10;

/**
 * Initial capacity. Defaults to {@value #DEFAULT_INITIAL_CAPACITY}.
 * @config
 */
private static final String SIMILARITY_LIST_LENGTH = "sim_list_length";

// --------------------------------------------------------------------------
// Fields
// --------------------------------------------------------------------------

/** Neighbors */
protected Node[] neighbors;

/** Actual number of neighbors in the array */
protected int len;

// --------------------------------------------------------------------------
// Initialization
// --------------------------------------------------------------------------

public OverlayLinkage(String s)
{
	neighbors = new Node[Configuration.getInt(s + "." + SIMILARITY_LIST_LENGTH,DEFAULT_INITIAL_CAPACITY)];
	//System.out.println("number of neighbors="+Configuration.getInt(s + "." + PAR_INITCAP,DEFAULT_INITIAL_CAPACITY));
	len = 0;
}

//--------------------------------------------------------------------------

public Object clone()
{
	OverlayLinkage ip = null;
	try { ip = (OverlayLinkage) super.clone(); }
	catch( CloneNotSupportedException e ) {} // never happens
	ip.neighbors = new Node[neighbors.length];
	System.arraycopy(neighbors, 0, ip.neighbors, 0, len);
	ip.len = len;
	return ip;
}

// --------------------------------------------------------------------------
// Methods
// --------------------------------------------------------------------------

public boolean contains(Node n)
{
	for (int i = 0; i < len; i++) {
		if (neighbors[i] == n)
			return true;
	}
	return false;
}

// --------------------------------------------------------------------------

/** Adds given node if it is not already in the network. There is no limit
* to the number of nodes that can be added. */
public boolean addNeighbor(Node n)
{
	for (int i = 0; i < len; i++) {
		if (neighbors[i] == n)
			return false;
	}
	if (len == neighbors.length) {
		Node[] temp = new Node[3 * neighbors.length / 2];
		System.arraycopy(neighbors, 0, temp, 0, neighbors.length);
		neighbors = temp;
	}
	neighbors[len] = n;
	len++;
	return true;
}

// --------------------------------------------------------------------------

public Node getNeighbor(int i)
{
	return neighbors[i];
}

// --------------------------------------------------------------------------

public int degree()
{
	return len;
}

// --------------------------------------------------------------------------

public void pack()
{
	if (len == neighbors.length)
		return;
	Node[] temp = new Node[len];
	System.arraycopy(neighbors, 0, temp, 0, len);
	neighbors = temp;
}

// --------------------------------------------------------------------------

public String toString()
{
	if( neighbors == null ) return "DEAD!";
	StringBuffer buffer = new StringBuffer();
	buffer.append("len=" + len + " maxlen=" + neighbors.length + " [");
	for (int i = 0; i < len; ++i) {
		buffer.append(neighbors[i].getIndex() + " ");
	}
	return buffer.append("]").toString();
}

// --------------------------------------------------------------------------

public void onKill()
{
	neighbors = null;
	len = 0;
}

}
