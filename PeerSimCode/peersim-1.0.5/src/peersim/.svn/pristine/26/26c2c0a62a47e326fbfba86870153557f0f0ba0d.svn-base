package peersim.simildis;

public class InfoEdge implements Comparable<InfoEdge>{
	
	public int peer_id_from;
	public int peer_id_to;
	public float transfer;
	
	public InfoEdge(int peer_id_from, int peer_id_to, float transfer) {
		super();
		this.peer_id_from = peer_id_from;
		this.peer_id_to = peer_id_to;
		this.transfer = transfer;
	}
	
	public String toString(){
		return peer_id_from+"->"+peer_id_to;
	}

	@Override
	public int compareTo(InfoEdge o2) {
		// TODO Auto-generated method stub
		
		if (this.peer_id_from==o2.peer_id_from && this.peer_id_to==o2.peer_id_to && this.transfer==o2.transfer)
			return 0;
		else
			return -1;
	}
	
	public boolean equals(InfoEdge o2){
		return (this.peer_id_from==o2.peer_id_from && this.peer_id_to==o2.peer_id_to && this.transfer==o2.transfer);
	}



}
