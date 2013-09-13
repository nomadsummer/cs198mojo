package peersim.ps.tman;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import peersim.ps.types.NodeInterests;
import peersim.ps.types.NodeProfile;
import peersim.ps.types.NodeUtility;
import peersim.ps.types.Topic;

public class Buffer {
	private final BigInteger N;
	private final NodeProfile selfProfile;
	private List<NodeProfile> peers = new ArrayList<NodeProfile>();
	private List<NodeProfile> currentView = new ArrayList<NodeProfile>();

//---------------------------------------------------------------------------------		
	public Buffer(List<NodeProfile> peers, NodeProfile selfProfile, BigInteger maxN) {
		NodeProfile np;
		
		for (int index = 0; index < peers.size(); index++) {
			np = new NodeProfile(peers.get(index));
			this.peers.add(np);
			this.currentView.add(np);
		}
		
		this.N = maxN;
		this.selfProfile = selfProfile;
	}

//---------------------------------------------------------------------------------		
	public void merge(List<NodeProfile> newPeers) {
		
		NodeProfile p;
		
		if (newPeers == null)
			return;
		
		for (int index = 0; index < newPeers.size(); index++) {
			p = newPeers.get(index);
			if (!this.peers.contains(p))
				this.peers.add(new NodeProfile(p));
			else {
				this.peers.set(this.peers.indexOf(p), p);
			}				
		}
	}	

//---------------------------------------------------------------------------------		
	public List<NodeProfile> selectView(int size, double fingerDistance) {
		// TODO: define the ranking function for sort
		if (size < 3)
			return null;
		else {
			List<NodeProfile> tempList = new ArrayList<NodeProfile>();
			List<NodeProfile> selectedView = new ArrayList<NodeProfile>();
			
			while (this.peers.contains(this.selfProfile))
				this.peers.remove(this.selfProfile);
			
			NodeProfile pred;			
			NodeProfile succ;
			NodeProfile finger;
			
			if (this.peers.size() > 2) {
				pred = this.peers.get(0);
				succ = this.peers.get(1);
			} else if (this.peers.size() > 1) {
				pred = this.peers.get(0);
				succ = this.peers.get(1);
			} else if (this.peers.size() > 0) {
				pred = this.peers.get(0);
				succ = this.selfProfile;
			} else {
				pred = this.selfProfile;			
				succ = this.selfProfile;
			}				
			
			int k = this.selfProfile.getAddress().getId().intValue();
			int p = pred.getAddress().getId().intValue();
			int s = succ.getAddress().getId().intValue();
			int i;
			for (NodeProfile np : this.peers) {
				i = np.getAddress().getId().intValue();
				if (((i > k) && (p > k) && (i > p)) || ((i < k) && (p > k)) || ((i < k) && (p < k) && (i > p))) {
					pred = np;
					p = pred.getAddress().getId().intValue();
				}
				
				if (((i > k) && (s > k) && (i < s)) || ((i > k) && (s < k)) || ((i < k) && (s < k) && (i < s))) {
					succ = np;
					s = succ.getAddress().getId().intValue();
				}	
			}
			
			selectedView.clear();
			selectedView.add(pred);
			selectedView.add(succ);
			
			// select the rest of the fingers according to the ranking function
			for (NodeProfile np : this.peers)
				if (np.getNodeInterests() != null && !tempList.contains(np) && np != pred && np != succ)
					tempList.add(np);
		
			int z = size - selectedView.size();
			if (tempList.size() > z) {
				List<NodeProfile> sortedPeers = sortPeers(tempList);
				
				tempList = sortedPeers.subList(0, z - 1);
				selectedView.addAll(tempList);
				tempList = sortedPeers.subList(z - 1 , sortedPeers.size());
				finger = findPeerAtDistance(fingerDistance, selectedView);
				if (finger != null) 
					selectedView.add(2, finger);
			}
			else
				selectedView.addAll(tempList);

			return selectedView;
		}
	}	

//---------------------------------------------------------------------------------
	public List<NodeProfile> getPeers() {
		return this.peers;
	}
	
//---------------------------------------------------------------------------------
	public NodeProfile findPeerAtDistance(double distance,List<NodeProfile> alreadySelected) {
		int f;
		int temp;
		int d, tempd;
		NodeProfile finger = null;
		int self = this.selfProfile.getAddress().getId().intValue();
		List<NodeProfile> notSelected = new ArrayList<NodeProfile>();

		for (NodeProfile p: this.peers) {
			if (!alreadySelected.contains(p))
				notSelected.add(p);
		}
		
		if (notSelected.isEmpty())
			return null;
		else {
			finger = notSelected.get(0);
			f = finger.getAddress().getId().intValue();
			d = Math.min(Math.abs(self - f), (this.N.intValue() - Math.abs(self - f)));			
			
			for (int i = 0; i < notSelected.size(); i++) {
				temp = notSelected.get(i).getAddress().getId().intValue();
				tempd = Math.min(Math.abs(self - temp), (this.N.intValue() - Math.abs(self - temp)));
				if (Math.abs(tempd - distance) < Math.abs(d - distance)) {
					finger = notSelected.get(i);
					f = finger.getAddress().getId().intValue();
					d = Math.min(Math.abs(self - f), (this.N.intValue() - Math.abs(self - f)));
				}
			}
		}
		
		return finger;	
	}
	
//---------------------------------------------------------------------------------
	public  ArrayList<NodeProfile> sortPeers(List<NodeProfile> peersToSort) {
		NodeUtility node;
		ArrayList<NodeUtility> list = new ArrayList<NodeUtility>();

		for (NodeProfile p: peersToSort) {
			node = new NodeUtility(getUtility(p), p);
			if (!list.contains(node))
				list.add(node);
		}
		
		// bubble sort
		NodeUtility temp;
		for (int i = 0; i < list.size() - 1; i++) {
			for (int j = i + 1; j < list.size(); j++) {
				if (list.get(j).getUtility() > list.get(i).getUtility()) {
					temp = list.get(i);
					list.set(i, list.get(j));
					list.set(j, temp);
				}
			}
		}
		
		ArrayList<NodeProfile> sortedList = new ArrayList<NodeProfile>();
		for (NodeUtility nu: list) {
			sortedList.add(nu.getNodeProfile());
		}
		
		return sortedList;		
	}
	
//---------------------------------------------------------------------------------	
	public float getUtility(NodeProfile p) {
		NodeInterests p_interests = p.getNodeInterests();
		NodeInterests selfInterests = this.selfProfile.getNodeInterests();
		
		Vector<Topic> t_self = new Vector<Topic>(selfInterests.getInterests().keySet());
		Vector<Topic> t_p = new Vector<Topic>(p_interests.getInterests().keySet());

		int intersection = 0;
		
		for (Topic t:t_self) {
			if (t_p.contains(t))
				intersection++;		
		}
		
		int union = t_self.size() + t_p.size() - intersection;
		float similarity = (intersection > 0 ? (intersection * 1000) / union : 0);
		int distance = (this.N.intValue() + p.getAddress().getId().intValue() - this.selfProfile.getAddress().getId().intValue()) % N.intValue();
		float utility = 10000 * similarity - distance;
		
		return utility;		
	}
}