package peersim.simildis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.Math;

import org.nfunk.jep.function.Power;

import peersim.graph.NeighbourListGraph;
import peersim.simildis.LeveldNode;


public class SimilarityDAG {
	
	public final NeighbourListGraph DAG;
	public  int root;
	public final HashMap<Integer, NodeLabels> labels;
	public float thetta;
	public int dummyNodeIndex;
	
	public final HashMap<Integer, Float> receivedSimilarity;
	
	public final HashMap<Integer, Float> flowToParents;
	public final HashMap<Integer, Float> flowChildren;
	
	
	public SimilarityDAG(NeighbourListGraph initialGraph, int rootNode, float thetta){
		this.DAG = new NeighbourListGraph(true);
		this.root = rootNode;
		this.labels = new HashMap<Integer, NodeLabels>();
		this.thetta = thetta;
		this.dummyNodeIndex = -1;
		this.receivedSimilarity = new HashMap<Integer, Float>();
		this.flowChildren = new HashMap<Integer, Float>();
		this.flowToParents = new HashMap<Integer, Float>();
		
	}
	
	public SimilarityDAG(int rootNode){
		this.DAG = new NeighbourListGraph(true);
		this.root = rootNode;
		this.labels = new HashMap<Integer, NodeLabels>();
		this.thetta = (float)0;
		this.receivedSimilarity = new HashMap<Integer, Float>();
		this.flowChildren = new HashMap<Integer, Float>();
		this.flowToParents = new HashMap<Integer, Float>();
		
	}
	
	public SimilarityDAG(){
		this.DAG = new NeighbourListGraph(true);
		this.root = -1;
		this.labels = new HashMap<Integer, NodeLabels>();
		this.thetta = (float)0;
		this.receivedSimilarity = new HashMap<Integer, Float>();
		this.flowChildren = new HashMap<Integer, Float>();
		this.flowToParents = new HashMap<Integer, Float>();
		
	}

	private void generateDAGIgnorance(NeighbourListGraph initGraph){
		/**
		 * This method generates a DAG from the provided graph. It INGORES the link between nodes in  the same level.  
		 */
		
		HashMap<Integer, Set<Integer>> reachableNodes = initGraph.bfsTraverseUndirected(this.root);
		int numberOfLevels = reachableNodes.keySet().size();
		
		this.labels.put(this.root, new NodeLabels((float)1.0,(byte)0));
		
		for (int i = 0; i < numberOfLevels-1; i++) {
			Integer[] headNodesArray = new Integer[reachableNodes.get(i).size()];
			headNodesArray = reachableNodes.get(i).toArray(headNodesArray);

			for (int j = 0; j < headNodesArray.length; j++) {
				int head = headNodesArray[j];
				
				for (Iterator<Integer> itJ = reachableNodes.get(i+1).iterator(); itJ.hasNext();) {
					int tail = itJ.next();
					if (initGraph.hasUnEdge(head, tail)) {
						this.DAG.setEdge(head, tail, initGraph.getBiDiEdgeWeights(head, tail) );
						
					}
				}
			}
			
			// updates edges weights in the DAG graph
			for (int j = 0; j < headNodesArray.length; j++) {
				int head = headNodesArray[j];
				float totalOutCapacity = this.DAG.totalOutCapacity(head);
				float headSimilarity = this.labels.get(head).similarity;
				for (Iterator<Integer> iterator = this.DAG.getNeighbours(head).iterator(); iterator.hasNext();) {
					int tail = iterator.next();
					float w = headSimilarity * this.DAG.getEdgeWeight(head, tail) / totalOutCapacity;
					this.DAG.updateWeight(head	, tail, w);
				}
			} // END: updates edges weights in the DAG graph
			
				// updates similarity values. 
				for (int j = 0; j < headNodesArray.length; j++) {
					int head = headNodesArray[j];
					for (Iterator<Integer> iterator = this.DAG.getNeighbours(head).iterator(); iterator.hasNext();) {
						int tail = iterator.next();
						float incomingWeight = this.DAG.totalInCapacity(tail);
						float tailSimilarity = (float)Math.pow(this.thetta, i+1) * incomingWeight;
						if (!this.labels.keySet().contains(tail)){
							this.labels.put(tail, new NodeLabels(tailSimilarity,(byte)(i+1)));
					}
										
				}
			} // END: updates similarity values. 
		}
	}
	
	
////////////////////////////////////////////////////////////////////////////////////////////
	public void generateDAGDummy(NeighbourListGraph initGraph){
		/**
		 * This method generates a DAG from the provided graph. It INGORES the link between nodes in  the same level.  
		 */
		
		HashMap<Integer, Set<Integer>> reachableNodes = initGraph.bfsTraverseUndirected(this.root);
		int numberOfLevels = reachableNodes.keySet().size();
		
		log("Reachable nodes:\n"+reachableNodes);
		this.labels.put(this.root, new NodeLabels((float)1.0,(byte)0));
		
		//int dummyNodeIndex = -1;
		for (int i = 0; i < numberOfLevels ; i++) {
			Integer[] headNodes = new Integer[reachableNodes.get(i).size()];
			headNodes = reachableNodes.get(i).toArray(headNodes);
			
			for (int j = 0; j < headNodes.length; j++) {
				int head = headNodes[j];
				if (i < numberOfLevels -1 ){
				for (Iterator<Integer> itJ = reachableNodes.get(i+1).iterator(); itJ.hasNext();) { // next level nodes
					int tail = itJ.next();
					if (initGraph.hasUnEdge(head, tail)) {
						this.DAG.setEdge(head, tail, initGraph.getBiDiEdgeWeights(head, tail) );
						
					}
				}
				}
				for (int k = j+1; k < headNodes.length; k++){
					int tail = headNodes[k];
					if (head!=tail){
						if (initGraph.hasUnEdge(head, tail)) { //edge at the same level
							float w = initGraph.getBiDiEdgeWeights(head, tail)/2;
							this.DAG.setEdge(head, this.dummyNodeIndex, w );
							this.DAG.setEdge(tail, this.dummyNodeIndex, w );
							this.dummyNodeIndex -=1;
							
						}
					}
				}
			}
			
			for (int j = 0; j < headNodes.length; j++) {
				int head = headNodes[j];
				float totalOutCapacity = this.DAG.totalOutCapacity(head);
				float headSimilarity = this.labels.get(head).similarity;
				for (Iterator<Integer> iterator = this.DAG.getNeighbours(head).iterator(); iterator.hasNext();) {
					int tail = iterator.next();
					float w = headSimilarity * this.DAG.getEdgeWeight(head, tail) / totalOutCapacity;
					this.DAG.updateWeight(head	, tail, w);
				}
			}

				for (int j = 0; j < headNodes.length; j++) {
					int head = headNodes[j];
					for (Iterator<Integer> iterator = this.DAG.getNeighbours(head).iterator(); iterator.hasNext();) {
						int tail = iterator.next();
						float incomingWeight = this.DAG.totalInCapacity(tail);
						float tailSimilarity = (float)Math.pow(this.thetta, i+1) * incomingWeight;
						if (!this.labels.keySet().contains(tail)){
							this.labels.put(tail, new NodeLabels(tailSimilarity,(byte)(i+1)));
					}
										
				}
			}
		}
		log("DAG generation, Ignoring Mtehod: DONE");
		log("DAG Graph:\n"+this.DAG.toString());
	}
	
	public static void log(String s){
		System.out.println(s);
	}
	
	
	public  static void readTrace(NeighbourListGraph g, String fileName){
		Scanner scanner;
		try {
			scanner = new Scanner(new FileReader(fileName));
			
			
			      while ( scanner.hasNextLine() ){
			    	  
			    	  String s = scanner.nextLine();
			    	  if (s.charAt(0)=='#')
			    		 continue;
			    	  String[] lineStr = s.split("\\|");
			    	  int a = Integer.parseInt(lineStr[0].replaceAll(" ", ""));
			    	  int b = Integer.parseInt(lineStr[1].replaceAll(" ", ""));
			    	  int c = Integer.parseInt(lineStr[2].replaceAll(" ", ""));
			    	  if (g.getEdgeWeight(a, b) >= c || a==0 || b==0) continue;
			    	  g.setEdge(a, b,c);
			    	  }
			
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
	
	Set<Integer> getDummyChildren(int node){
		/*
		 * returns the dummy children of a node
		 */
		
		Set<Integer> result = new HashSet<Integer>();
		Collection<Integer> outgoingNeighbors = this.DAG.getNeighbours(node);
		for(Integer neighbor:outgoingNeighbors){
			if (neighbor<0){
				result.add(neighbor);
			}
		}
		
		return result;
	}
	
	int getTheOtherParent(int dummyNode, int parent1){
		if (dummyNode >=0 ) return -1; //it is not a dummy node
		Collection<Integer> parents = this.DAG.getReverseNeighbours(dummyNode);
		for(Integer par:parents)
			if (par != parent1)
				return par;
		return -1;
	}
	
	public int hasCycle(){
		// checks whether the graph has cycle or not.
		
		if (this.root==-1) return -1;
		Set<Integer> traversed = new HashSet<Integer>();
		Queue<Integer> q = new LinkedList<Integer>();
		
		q.add(this.root);
		while (!q.isEmpty()){
			int i = q.remove();
			for (Integer c : this.DAG.getNeighbours(i)) {
				if (traversed.contains(c)){
					log("cycle parent"+i);
					return c;}

				q.add(c);
			}
			traversed.add(i);			
		}
		
		return -1;
		
	}
	
	public static void main(String args[])
	{
		NeighbourListGraph g = new NeighbourListGraph(true);
		readTrace(g,"/Users/rahimdelaviz/Documents/workspace/SimDisRunTime/src/peersim/simildis/traces/sample_graph_2.txt");
		
		//log("Initial Graph:\n"+g.toString());
		SimilarityDAG sg = new SimilarityDAG(g , 1, (float) 0.8);
		
		sg.generateDAGDummy(g);
		log(sg.labels.toString());
		
		//sg.DAG.setEdge(9, 6,0.1f);
		
		System.out.println(sg.hasCycle());
		g.visualize("src/peersim/simildis/result/SG.dot");
		sg.DAG.visualize("src/peersim/simildis/result/DAGSim.dot");
		
		g.logDegreeDist("src/peersim/simildis/result/DegreeDist.txt");
		
	}

}
