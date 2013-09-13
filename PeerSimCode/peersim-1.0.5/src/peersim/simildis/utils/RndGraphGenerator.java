package peersim.simildis.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.Set;


import peersim.graph.NeighbourListGraph;

public class RndGraphGenerator {
	
	int numberOfNodes ;
	int numberOfEdges ;
	float averageDegree;
	boolean directed;
	
	NeighbourListGraph g = new NeighbourListGraph(true);
	
	public RndGraphGenerator(int numberOfNodes, int numberOfEdges, boolean directed) {
		super();
		this.numberOfNodes = numberOfNodes;
		this.numberOfEdges = numberOfEdges;
		this.directed = directed;
		this.averageDegree = (float)(numberOfEdges * 2.0 /numberOfNodes);
		
	}

	public RndGraphGenerator(int numberOfNodes, float averageDegree, boolean directed) {
		super();
		this.numberOfNodes = numberOfNodes;
		this.averageDegree = averageDegree;
		this.numberOfEdges = (int)(averageDegree * numberOfNodes / 2);
		this.directed = directed;
	}
	
	public void generateGraph(int seed, String fileName, int lineLength){
		
		FileOutputStream myFile;
		try {
			myFile = new FileOutputStream(fileName);
			PrintStream out = new PrintStream(myFile);
			out.println("#peer_id_from | peer_id_to | transfer");
			
			for (int i = 1; i <= this.numberOfNodes; i++) {
				this.g.addNode(i);
				
			}
			Set<Integer> nodes = g.getNodes() ;
			Integer[] nodesArray = new Integer[nodes.size()];
			nodesArray = nodes.toArray(nodesArray);
			
			Random r = new Random(seed);
			int arraySize = nodes.size();
			String s="";
			int k = 0;
			while ( k < this.numberOfEdges) {
				int i = r.nextInt(arraySize);
				
				int j = r.nextInt(arraySize);
				if (i!=j){
					int w = r.nextInt(1000);
					g.setEdge(nodesArray[i], nodesArray[j], w);
					s="";
					s+=nodesArray[i]+"|"+nodesArray[j]+"|"+w+"|";
					int s_length = s.length();
					for (int l = 0; l < (lineLength - s_length)-1 ; l++) {
						s+=" ";
					
					}
					out.println(s);
				}
				k+=1;
				
				
			}
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
	
	public static void main(String arags[]){
		RndGraphGenerator rndg = new RndGraphGenerator(200, 1000, true);
		rndg.generateGraph(21293, "src/peersim/simildis/traces/RndGraph_Trace_200.txt", 24);
		rndg.g.visualize("src/peersim/simildis/traces/RndGraph.txt");
	}
	
	

}
