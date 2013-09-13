package peersim.simildis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import peersim.graph.NeighbourListGraph;
import peersim.simildis.SimilarityList.NodeSim;


public class SubjectiveGraph {
	
	public final NeighbourListGraph sg; //directed subjective graph
	
	//public NeighbourListGraph DAG;
	
    /** Creates a new instance of BasicDirectedGraph */
    public SubjectiveGraph() {      
    	sg = new NeighbourListGraph(true);
    }
  
    
    public boolean addOrUpdateEdge(int peer_id_from, int peer_id_to, int weight){
    	return this.sg.updateWeight(peer_id_from, peer_id_to, weight);
    	
    }
    
    public String toString(){
    	return sg.toString();
    }
    
    
    
}

