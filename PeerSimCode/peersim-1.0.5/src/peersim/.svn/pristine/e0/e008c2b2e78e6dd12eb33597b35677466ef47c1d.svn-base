package peersim.simildis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.lang.Math;

import peersim.Simulator;
import peersim.util.*;
import peersim.simildis.utils.*;

import peersim.graph.NeighbourListGraph;
import peersim.rangesim.*;



public class SimilarityDAGNoDummy {
	
	public final NeighbourListGraph DAG;
	public  int root;
	public final HashMap<Integer, NodeLabels> labels;
	public float thetta;
	public int dummyNodeIndex;
	
	public SimilarityDAGNoDummy(NeighbourListGraph initialGraph, int rootNode, float thetta){
		this.DAG = new NeighbourListGraph(true);
		this.root = rootNode;
		this.labels = new HashMap<Integer, NodeLabels>();
		this.thetta = thetta;
		this.dummyNodeIndex = -1;
		
	}
	
	public SimilarityDAGNoDummy(int rootNode){
		this.DAG = new NeighbourListGraph(true);
		this.root = rootNode;
		this.labels = new HashMap<Integer, NodeLabels>();
		this.thetta = (float)0;
		
		
	}
	
	public SimilarityDAGNoDummy(){
		this.DAG = new NeighbourListGraph(true);
		this.root = -1;
		this.labels = new HashMap<Integer, NodeLabels>();
		this.thetta = (float)0;
		
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
	
	private HashMap<Integer, Set<Integer>> findConnectedComponents(List<Integer> nodes, NeighbourListGraph initGraph){
		
		if (nodes.size()==0) return null;
		
		HashMap<Integer, Set<Integer>> result = new HashMap<Integer, Set<Integer>>();
		
		int cCounter = 0;
		//result.put(cCounter, new HashSet<Integer>());
		//result.get(cCounter).add(nodes.get(0));
		//nodes.remove(0);
		
		while (nodes.size()>0){
			for(Integer i: nodes){
				result.put(cCounter, new HashSet<Integer>());
				HashSet<Integer> tempSet = new HashSet<Integer>();
				
				Collection<Integer> neighbors = initGraph.getNeighbours(i);
				for(Integer n: neighbors){
					if (nodes.contains(n)){
						result.get(cCounter).add(n);
						nodes.remove(n);
					}
				}
			}
		}
		
		
		while (nodes.size()>0){
			
			for(Integer n: nodes){
			//	initGraph.getNeighbours(i)
				
			}
			result.put(cCounter, new HashSet<Integer>());
			
			
			cCounter +=1;
		}
		
		return result;
	}
////////////////////////////////////////////////////////////////////////////////////////////
	public void generateDAGDummyNewFormula(NeighbourListGraph initGraph){
		/**
		 * This method treats the nodes in the same level differently.  
		 */
		
		HashMap<Integer, Set<Integer>> reachableNodes = initGraph.bfsTraverseUndirected(this.root);
		int numberOfLevels = reachableNodes.keySet().size();
		
		//log("Reachable nodes:\n"+reachableNodes);
		this.labels.put(this.root, new NodeLabels((float)1.0,(byte)0));
		
		for (int i = 0; i < numberOfLevels ; i++) {
			Integer[] headNodes = new Integer[reachableNodes.get(i).size()];
			headNodes = reachableNodes.get(i).toArray(headNodes);
			
			//Start: Building the DAG graph until the level i+1 
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

			}
			
		balanceWeightsAverage(initGraph, reachableNodes, i);
			// End: building the DAG graph until the level i+1
			// Calculating the edge weights
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
			
			// Calculating the similarity in the level i+1
			//System.out.println(this.DAG.toString());
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
		//log("DAG generation, Ignoring Mtehod: DONE");
		//log("DAG Graph:\n"+this.DAG.toString());
	}
	
	///////////////////////////////////
	
	public void balanceWeightsAverage(NeighbourListGraph sGraph, HashMap<Integer, Set<Integer>> reachableNodes, int myLevel ){
		
		FileOutputStream myFile;
		PrintStream out;

		//try {
			Set<Integer> tempSet = new HashSet<Integer>();
			List<SimpleEdge> sameLevelEdges = new ArrayList<SimpleEdge>();
			for(Integer i: reachableNodes.get(myLevel)){
				for(Integer j: reachableNodes.get(myLevel)){
					if (i!=j){
						if (!sameLevelEdges.contains(new SimpleEdge(i , j))) {
							if (sGraph.hasUnEdge(i, j))
								sameLevelEdges.add(new SimpleEdge(i, j) );
							
						} 
					}
					
				}
			}
			
			if ( sameLevelEdges.isEmpty() ) return;
			
			/*myFile = new FileOutputStream("src/peersim/simildis/result/convergenceSum.txt");
			out = new PrintStream(myFile);
			out.println("#iteration|nodeId|similarity");
			

			
			for(SimpleEdge e: sameLevelEdges){
				int node1 = e.i;
				int node2 = e.j;

				if(!tempSet.contains(node1)) {
					out.println("before|"+node1+"|"+this.labels.get(node1).similarity);
					tempSet.add(node1);
					
				}
				
				if(!tempSet.contains(node2)) {
					out.println("before|"+node2+"|"+this.labels.get(node2).similarity);
					tempSet.add(node2);
					
				}
			}
			out.flush();*/
			
		
		for (int i = 0; i < 1; i++) {
		
			HashMap<Integer, List<Float>> similarities = new HashMap<Integer, List<Float>>();
			for(SimpleEdge e: sameLevelEdges){
				int node1 = e.i;
				int node2 = e.j;
				
				float commonEdgeWeight = sGraph.getBiDiEdgeWeights(node1, node2);		
				
				////////////////////////
				float capToChildren1 = 0; 
				tempSet.clear();
				Collection<Integer> parents1 = this.DAG.getReverseNeighbours(node1);
				float flowFromParents1 = 0;
				
				for(int neighbor : sGraph.getNeighbours(node1)){
					if (!parents1.contains(neighbor) && !tempSet.contains(neighbor) && neighbor!=node2){

						if (reachableNodes.get(myLevel).contains(neighbor))
							capToChildren1 += sGraph.getBiDiEdgeWeights(node1, neighbor)/2;
						else
						capToChildren1 += sGraph.getBiDiEdgeWeights(node1, neighbor);
						tempSet.add(neighbor);
					}
					if (parents1.contains(neighbor) && !tempSet.contains(neighbor)){
						
						flowFromParents1 += sGraph.getBiDiEdgeWeights(node1, neighbor);
						tempSet.add(neighbor);
					}
				}
				
				
				for(int neighbor : sGraph.getReverseNeighbours(node1)){
					if (!parents1.contains(neighbor) && !tempSet.contains(neighbor) && neighbor!=node2){

						if (reachableNodes.get(myLevel).contains(neighbor))
							capToChildren1 += sGraph.getBiDiEdgeWeights(node1, neighbor)/2;
						else
						capToChildren1 += sGraph.getBiDiEdgeWeights(node1, neighbor);
						tempSet.add(neighbor);
					}
					if (parents1.contains(neighbor) && !tempSet.contains(neighbor)){
						
						flowFromParents1 += sGraph.getBiDiEdgeWeights(node1, neighbor);
						tempSet.add(neighbor);
					}
					
				}
				
				/////////////////////////
				tempSet.clear();
				float capToChildren2 = 0; 
				Collection<Integer> parents2 = this.DAG.getReverseNeighbours(node2);
				float flowFromParents2 = 0;
				for(int neighbor : sGraph.getNeighbours(node2)){
					if (!parents2.contains(neighbor) && !tempSet.contains(neighbor) && neighbor!=node1){

						if (reachableNodes.get(myLevel).contains(neighbor))
							capToChildren2 += sGraph.getBiDiEdgeWeights(node2, neighbor)/2;
						else
						capToChildren2 += sGraph.getBiDiEdgeWeights(node2, neighbor);
						
						tempSet.add(neighbor);
					}
					
					if (parents2.contains(neighbor) && !tempSet.contains(neighbor)){
						flowFromParents2 += sGraph.getBiDiEdgeWeights(node2, neighbor);
						tempSet.add(neighbor);
					}
				}
				
				for(int neighbor : sGraph.getReverseNeighbours(node2)){
					if (!parents2.contains(neighbor) && !tempSet.contains(neighbor) && neighbor!=node1){
						if (reachableNodes.get(myLevel).contains(neighbor))
							capToChildren2 += sGraph.getBiDiEdgeWeights(node2, neighbor)/2;
						else
						capToChildren2 += sGraph.getBiDiEdgeWeights(node2, neighbor);
						tempSet.add(neighbor);
					}
					if (parents2.contains(neighbor) && !tempSet.contains(neighbor)){

						flowFromParents2 += sGraph.getBiDiEdgeWeights(node2, neighbor);
						tempSet.add(neighbor);
					}
				}

				float share1 = 1- (flowFromParents1/(flowFromParents1+flowFromParents2) );
				float share2 = 1- (flowFromParents2/(flowFromParents1+flowFromParents2) );
				
				float w = commonEdgeWeight/2;
				float shareNode1 = (float) Math.pow(this.thetta, myLevel+1) * share1 * this.labels.get(node1).similarity * (w/(capToChildren1+w));
				float shareNode2 = (float) Math.pow(this.thetta, myLevel+1) * share2 * this.labels.get(node2).similarity * (w/(capToChildren2+w));
				
				float delta1 = (- shareNode1 + shareNode2)/2;
				float delta2 = - delta1;
				
				if (!similarities.containsKey(node1))
					similarities.put(node1, new ArrayList<Float>());
				
				if (!similarities.containsKey(node2))
					similarities.put(node2, new ArrayList<Float>());
				
				similarities.get(node1).add(delta1);
				similarities.get(node2).add(delta2);
				
			}
			
			for(Integer node: similarities.keySet()){
				float sum=0;
				float currentSim = this.labels.get(node).similarity;
				for(float delta: similarities.get(node)){
					sum+= delta;
					currentSim += delta;
				}
				
				//out.println("after|"+node+"|"+ currentSim);
				this.labels.remove(node);
				this.labels.put(node, new NodeLabels(currentSim, (byte)(myLevel)));
				
			}
			//out.flush();
		}
		//} 
		//catch (FileNotFoundException e1) {
			
		//	e1.printStackTrace();
		//}
			
	}
	
	
	///////////////////////////////////
	public void balanceWeights(List<SimpleEdge> compEdges , NeighbourListGraph sGraph, HashMap<Integer, Set<Integer>> reachableNodes, int myLevel ){
		
		FileOutputStream myFile;
		PrintStream out;
		if  (compEdges.isEmpty()) 
			return;
		try {
			myFile = new FileOutputStream("src/peersim/simildis/result/convergence.txt");
			out = new PrintStream(myFile);
			out.println("#iteration/nodeId/similarity");
			
			float sumSim = 0;
			Set<Integer> tempSet = new HashSet<Integer>();
			for(SimpleEdge e: compEdges){
				int node1 = e.i;
				int node2 = e.j;

				if(!tempSet.contains(node1)) {
					out.println(0+"|"+node1+"|"+this.labels.get(node1).similarity);
					tempSet.add(node1);
					sumSim += this.labels.get(node1).similarity;
				}
				
				if(!tempSet.contains(node2)) {
					out.println(0+"|"+node2+"|"+this.labels.get(node2).similarity);
					tempSet.add(node2);
					
					sumSim += this.labels.get(node2).similarity;
					
				}
				
			
			}
			//out.println(0+"|"+"sum"+"|"+ sumSim);
			out.flush();
			
		///////////////
		
		for (int i = 0; i < 500; i++) {
		
			Collections.shuffle(compEdges);
			tempSet.clear();
			for(SimpleEdge e: compEdges){
				int node1 = e.i;
				int node2 = e.j;
				float commonEdgeWeight = sGraph.getBiDiEdgeWeights(node1, node2);		
				
				////////////////////////

				float capToChildren1 = 0; 
				Collection<Integer> parents1 = this.DAG.getReverseNeighbours(node1);
				
				tempSet.clear();
				for(int neighbor : sGraph.getNeighbours(node1)){
					//if (!parents1.contains(neighbor) && !tempSet.contains(neighbor) && neighbor!=node2){
					if (!tempSet.contains(neighbor) && neighbor!=node2){
						capToChildren1 += sGraph.getBiDiEdgeWeights(node1, neighbor);
						tempSet.add(neighbor);
					}
				}
				
				for(int neighbor : sGraph.getReverseNeighbours(node1)){
					//if (!parents1.contains(neighbor) && !tempSet.contains(neighbor) && neighbor!=node2){
					if (!tempSet.contains(neighbor) && neighbor!=node2){
						capToChildren1 += sGraph.getBiDiEdgeWeights(node1, neighbor);
						tempSet.add(neighbor);
					}
				}
				
				/////////////////////////
				tempSet.clear();
				float capToChildren2 = 0; 
				Collection<Integer> parents2 = this.DAG.getReverseNeighbours(node2);
				for(int neighbor : sGraph.getNeighbours(node2)){
					//if (!parents2.contains(neighbor) && !tempSet.contains(neighbor) && neighbor!=node1){
					if (!tempSet.contains(neighbor) && neighbor!=node1){
						capToChildren2 += sGraph.getBiDiEdgeWeights(node2, neighbor);
						tempSet.add(neighbor);
					}
				}
				for(int neighbor : sGraph.getReverseNeighbours(node2)){
					//if (!parents2.contains(neighbor) && !tempSet.contains(neighbor) && neighbor!=node1){
					if (!tempSet.contains(neighbor) && neighbor!=node1){
						capToChildren2 += sGraph.getBiDiEdgeWeights(node2, neighbor);
						tempSet.add(neighbor);
					}
				}

				float w = (float) (commonEdgeWeight/Math.pow(2,i));
				float shareNode1 = (float)Math.pow(this.thetta, myLevel+1) * this.labels.get(node1).similarity * (w/(capToChildren1+w));
				float shareNode2 = (float)Math.pow(this.thetta, myLevel+1) * this.labels.get(node2).similarity * (w/(capToChildren2+w));
				
				
				
				float newSimNode1 = this.labels.get(node1).similarity - shareNode1 + (shareNode1+shareNode2)/2;
				float newSimNode2 = this.labels.get(node2).similarity - shareNode2 + (shareNode1+shareNode2)/2;
				
				this.labels.remove(node1);
				this.labels.remove(node2);
				
				this.labels.put(node1, new NodeLabels(newSimNode1, (byte)(myLevel)));
				this.labels.put(node2, new NodeLabels(newSimNode2, (byte)(myLevel)));
			}
			
			sumSim = 0;
			tempSet.clear();
			for(SimpleEdge e: compEdges){
				int node1 = e.i;
				int node2 = e.j;
				//Set<Integer> tempSet = new HashSet<Integer>();
				if(!tempSet.contains(node1)) {
					out.println((i+1)+"|"+node1+"|"+this.labels.get(node1).similarity);
					tempSet.add(node1);
					sumSim += this.labels.get(node1).similarity;
				}
				
				if(!tempSet.contains(node2)) {
					out.println((i+1)+"|"+node2+"|"+this.labels.get(node2).similarity);
					tempSet.add(node2);
					
					sumSim += this.labels.get(node2).similarity;
					
				}
				
			
			}
			//out.println((i+1)+"|"+"sum"+"|"+ sumSim);
			out.flush();
		}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
			
	}
	
	
	public static void log(String s){
		System.out.println(s);
	}
	
	
	public  static List<WeightedEdge > readTrace(NeighbourListGraph g, String fileName, int numberOfLinesToRaed){
		Scanner scanner;
		int lineKey = 0;
		int numberOfLines = 1;
		List<WeightedEdge> edges = new ArrayList<WeightedEdge>();
		try {
			scanner = new Scanner(new FileReader(fileName));
			
			
			      while ( scanner.hasNextLine() && numberOfLines <=numberOfLinesToRaed){
			    	  
			    	  String s = scanner.nextLine();
			    	  if (s.charAt(0)=='#')
			    		 continue;
			    	  numberOfLines +=1;
			    	  String[] lineStr = s.split("\\|");
			    	  int a = Integer.parseInt(lineStr[0].replaceAll(" ", ""));
			    	  int b = Integer.parseInt(lineStr[1].replaceAll(" ", ""));
			    	  int c = Integer.parseInt(lineStr[2].replaceAll(" ", ""));
			    	  edges.add(new WeightedEdge(a,b,c));
			    	  lineKey ++;
			    	  if (g.getEdgeWeight(a, b) >= c || a==0 || b==0) continue;
			    	  g.setEdge(a, b,c);
			    	  //edges.put(lineKey, new WeightedEdge(a,b,c));
			    	  
			    	  }
			      	
			
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return edges;
	}
	
	
	
	/*class SimpleEdge implements Comparable<SimpleEdge>{
		private  final int i;
		public int getI() {
			return i;
		}

		public int getJ() {
			return j;
		}

		private final int j;
		SimpleEdge(int i , int j){
			this.i = i;
			this.j = j;
		}	
		
		
		@Override
		public boolean equals(Object o2){
			
			return ( this.i==((SimpleEdge)o2).i &&  this.j==((SimpleEdge)o2).j ) || ( this.i==((SimpleEdge)o2).j &&  this.j==((SimpleEdge)o2).i ) ;
		}
		
		@Override
		public int compareTo(SimpleEdge o2) {
				return 0;
		}
		public String toString(){
			return "("+i+","+j+")";
		}
		
		@Override
		public int hashCode(){
			return (10711 * (10711 * getI()+getJ())) + (10711 * (10711 * getJ()+getI())); 
		}
	}
	
	class WeightedEdge extends SimpleEdge{
		private final float w;
		public float getW() {
			return w;
		}
		WeightedEdge(int i, int j, float weight) {
			super(i, j);
			// TODO Auto-generated constructor stub
			this.w = weight;
		}
		
	}*/
	
	
	
	
	/*public static void main(String args[])
	{	
		NeighbourListGraph g = new NeighbourListGraph(true);
		readTrace(g,"/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilarityBasedDist/src/peersim/simildis/traces/scale_free_graph_2000.txt");
		
		SimilarityDAGNoDummy sg = new SimilarityDAGNoDummy(g , 1 , (float) 0.80);
		sg.generateDAGDummyNewFormula(g);
		g.visualize("src/peersim/simildis/result/SG_Static.dot");
		sg.DAG.visualize("src/peersim/simildis/result/DAGSim_Static.dot");
		
	}*/
	
	public  static void AmendConfigFile(String confFile , String traceFileName , int numberOfRecords, int networkSize){
		
		FileOutputStream confFileHandler;
		try {
			confFileHandler = new FileOutputStream(confFile);
			PrintStream outConf = new PrintStream(confFileHandler);
			outConf.println("##################################################################");
			outConf.println("random.seed 676");
			outConf.println("simulation.cycles "+(numberOfRecords+1));
			outConf.println("network.size "+(networkSize+1));
			outConf.println("protocol.lnk IdleProtocol");
			outConf.println("protocol.simildis peersim.simildis.SimilDis");
			
			outConf.println("##################################################################");
			outConf.println("protocol.simildis.start 150");
			outConf.println("protocol.simildis.linkable lnk");
			outConf.println("protocol.simildis.sim_list_length 10");
			outConf.println("protocol.simildis.buffer_capacity 3");
			outConf.println("protocol.simildis.num_times_to_send  3");
			outConf.println("protocol.simildis.fan_size 2");
			
			outConf.println("##################################################################");
			outConf.println("control.1tr peersim.simildis.TraceReader");
			outConf.println("control.1tr.protocol simildis");
			outConf.println("control.1tr.file_name  "+ traceFileName);
			outConf.println("control.1tr.line_per_cycle 1");
			outConf.println("control.1tr.total_lines "+numberOfRecords);
			outConf.println("control.1tr.header_length 38");
			outConf.println("control.1tr.line_length 24");
			
			outConf.println("##################################################################");
			outConf.println("control.4go peersim.simildis.GraphObserver");
			outConf.println("control.4go.protocol simildis");

			
			outConf.println("##################################################################");
			outConf.println("control.3su peersim.simildis.SimUpdatControl");
			outConf.println("control.3su.protocol simildis");
			outConf.println("control.3su.thetta 0.8");
			outConf.println("control.3su.strategy dummy");
			outConf.println("control.3su.dummy_share 0.5");
			
			outConf.println("##################################################################");
			outConf.println("control.5slu peersim.simildis.SimilarityListObserver");
			outConf.println("control.5slu.protocol simildis");
			outConf.println("init.rnd WireZero");
			outConf.println("init.rnd.protocol lnk");
			outConf.println("include.init rnd");
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public static void main(String args[])
	{	
		
		String traceFileName = args[0];   // the name of the trace file
		int numberOfLines = Integer.parseInt(args[1]); // number of lines in the trace
		String resultFile = args[2];  // the result file to log.
		int numberOfRuns = Integer.parseInt(args[3]);
		
		
		long totalTime, startTime, endTime;
		HashMap<Integer, IncrementalStats> statKeeper = new HashMap<Integer, IncrementalStats>(); 
		HashMap<Integer, Integer> numNodes = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> numEdges = new HashMap<Integer, Integer>();
		
		NeighbourListGraph gTemp = new NeighbourListGraph(true);
		List<WeightedEdge> edges = readTrace(gTemp, traceFileName, numberOfLines);
		//System.out.println("Num of read recs: "+edges.size()+"\n");
		try {
			for (int k = 0 ; k< numberOfRuns ; k++){
				
				int numOfRecs = edges.size();
				double accRuntime = 0;
				for(int i=0; i < numOfRecs; i++){
					
					if (!statKeeper.keySet().contains(i)){
						statKeeper.put(i, new IncrementalStats());
						
					}
					
					NeighbourListGraph g = new NeighbourListGraph(true);
					for(int j=0; j <= i; j++){

							if (g.getEdgeWeight(edges.get(j).i, edges.get(j).j) < edges.get(j).w)
								g.setEdge(edges.get(j).i, edges.get(j).j, edges.get(j).w);
					}
	
					SimilarityDAGNoDummy sg = new SimilarityDAGNoDummy(g , 1 , (float) 0.80);
					startTime = System.currentTimeMillis();
					sg.generateDAGDummyNewFormula(g);
					endTime = System.currentTimeMillis();
					double runtime = (endTime - startTime);
					numNodes.put(i, g.getNumberOfNodes());
					numEdges.put(i, g.getNumberOfEdges());
					statKeeper.get(i).add(runtime);
					if (i %500==0)
						log("run number"+k +"\t record number="+i+"\n");
					
			}
			log("run number:"+ k+" done.\n");
			}
			FileOutputStream outHandler = new FileOutputStream(resultFile);
			PrintStream out = new PrintStream(outHandler);
			out.println("#num_records|num_nodes|num_edges|density|avg_runtime|se_runtime");
			
			
			for (int key=0 ; key < statKeeper.keySet().size(); key++) {
				float density = (float) (2.0 * numEdges.get(key) / (numNodes.get(key) * (numNodes.get(key)-1)));
				String s = (key+1)+"|"+numNodes.get(key)+"|"+numEdges.get(key)+"|"+ density+"|"+statKeeper.get(key).getAverage()+"|"+statKeeper.get(key).getStE();
				out.println(s);
				
			}
			
			} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			/*String[] conf = new String[1];
			conf[0] = simdisConfFile;
			
			String[] param = new String[1];
			param[0] = simdisConfFile;
			RangeSimulator s = new RangeSimulator(param);
			s.run();*/
			
			
		//	}
	}
}
