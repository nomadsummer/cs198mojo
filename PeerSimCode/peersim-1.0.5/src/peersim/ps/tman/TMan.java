package peersim.ps.tman;

import java.math.BigInteger;
import java.util.Vector;

import peersim.core.*;
import peersim.config.Configuration;
import peersim.cdsim.CDProtocol;
import peersim.ps.newscast.Newscast;
import peersim.ps.types.NodeInterests;
import peersim.ps.types.NodeProfile;
import peersim.ps.types.PeerAddress;
import peersim.ps.types.View;

public class TMan implements CDProtocol {
	private static final String PAR_PROT = "protocol";
	private static final String PAR_VIEWSIZE = "viewSize";
	
    private final int pid;	
	private final String prefix;
	private Newscast newscast;
	
	private PeerAddress self;
	private View view = null;
	private int viewSize;
	private BigInteger maxN;
	private double fingerDistance;
	
	private NodeProfile selfProfile;
	
//------------------------------------------------------------------------
	public TMan(String prefix) {
		this.prefix = prefix;
		this.pid = Configuration.getPid(prefix + "." + PAR_PROT);
		this.viewSize = Configuration.getInt(prefix + "." + PAR_VIEWSIZE);
	}
	
//------------------------------------------------------------------------
	public Object clone() {
		TMan tman = new TMan(this.prefix);
		return tman;
	}

//------------------------------------------------------------------------
    public void init(Node node, BigInteger nodeId, int idLength) {
    	this.self = new PeerAddress(node, nodeId);
    	this.newscast = (Newscast)node.getProtocol(this.pid);
    	this.maxN = (new BigInteger(2 + "")).pow(idLength);
		this.fingerDistance = Math.exp(Math.log(maxN.doubleValue() / 2) * (CommonState.r.nextDouble() - 1)) * maxN.doubleValue() / 2;
    }

//------------------------------------------------------------------------
    public void initProfile(NodeProfile profile) {
       	this.selfProfile = profile;
    }

//------------------------------------------------------------------------
    public View getView() {
       	return this.view;
    }

//------------------------------------------------------------------------
    public NodeInterests getNodeInterests() {
    	return this.selfProfile.getNodeInterests();
    }
    
//------------------------------------------------------------------------
    public PeerAddress getPeerAddress() {
    	return this.self;
    }

//------------------------------------------------------------------------
    public PeerAddress getSucc() {
    	if (this.view != null)
    		return this.view.getSucc(self);    	
    	
    	return null;
    }

//------------------------------------------------------------------------
    public PeerAddress getPred() {
    	if (this.view != null)
    		return this.view.getPred(self);
    	
    	return null;
    }

//------------------------------------------------------------------------
    public Vector<PeerAddress> getCluster() {
    	if (this.view != null)
    		return this.view.getCluster();
    	
    	return null;
    }

//------------------------------------------------------------------------
    public void nextCycle(Node node, int protocolID) {
    	if (this.view != null && this.view.getSize() > 0) {
			PeerAddress selectedPeer = this.view.selectRandomPeer().getAddress();

			Buffer buffer = new Buffer(this.view.getNodes(), this.selfProfile, this.maxN);
			buffer.merge(this.peerSampling(protocolID));

			// replace that selected peer's entry with your own
			buffer.getPeers().remove(selectedPeer);
			buffer.getPeers().add(this.selfProfile);
			
			// send this buffer to the selected peer
			Node receiver = selectedPeer.getNode();
			Vector<NodeProfile> repliedBuffer = ((TMan)receiver.getProtocol(protocolID)).passiveThread(this.self, buffer, protocolID);

			// send this buffer to the succ
			Vector<NodeProfile> succRepliedBuffer = null;
			PeerAddress succ = this.getSucc();
			Node succNode = succ.getNode();
			
			if (!selectedPeer.equals(succ)) 
				succRepliedBuffer = ((TMan)succNode.getProtocol(protocolID)).passiveThread(this.self, buffer, protocolID);
			
			buffer.merge(repliedBuffer);
			if (!selectedPeer.equals(succ))
				buffer.merge(succRepliedBuffer);
						
			while (buffer.getPeers().contains(this.selfProfile))
				buffer.getPeers().remove(this.selfProfile);
			
			this.view.refresh(new Vector<NodeProfile>(buffer.selectView(this.viewSize, this.fingerDistance)));			
		}
		else {
			Vector<NodeProfile> neighbors = this.peerSampling(protocolID);	        
	        if (this.view == null)
	        	this.view = new View(neighbors, this.viewSize);
	        else
	        	this.view.refresh(neighbors);
		}
    }

//------------------------------------------------------------------------
    public Vector<NodeProfile> passiveThread(PeerAddress sender, Buffer receivedBuffer, int protocolID) {
		Buffer tempBuffer = new Buffer(this.view.getNodes(), this.selfProfile, this.maxN);
		tempBuffer.merge(this.peerSampling(protocolID));

    	Buffer buffer = new Buffer(tempBuffer.getPeers(), this.selfProfile, this.maxN);
		buffer.merge(receivedBuffer.getPeers());

		while (buffer.getPeers().contains(this.selfProfile))
			buffer.getPeers().remove(this.selfProfile);
		
		this.view.refresh(new Vector<NodeProfile>(buffer.selectView(this.viewSize, this.fingerDistance)));
		
		// replace that selected peer's entry with your own
		tempBuffer.getPeers().remove(sender);
		tempBuffer.getPeers().add(this.selfProfile);
		
    	return new Vector<NodeProfile>(tempBuffer.getPeers());
    }
    
//------------------------------------------------------------------------
    private Vector<NodeProfile> peerSampling(int protocolID) {
		Node node;
		TMan tman;
		Vector<NodeProfile> neighbors = new Vector<NodeProfile>();
		
		int num = Math.min(this.newscast.degree(), this.viewSize);
        
		for (int i = 0; i < num; i++) {
        	node = this.newscast.getNeighbor(i);
        	tman = (TMan)node.getProtocol(protocolID);
        	neighbors.add(new NodeProfile(new PeerAddress(node, tman.getPeerAddress().getId()), tman.getNodeInterests()));
		}
		
		return neighbors;
    }
}
