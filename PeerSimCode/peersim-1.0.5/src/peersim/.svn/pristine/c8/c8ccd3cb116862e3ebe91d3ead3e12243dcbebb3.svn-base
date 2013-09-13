package peersim.simildis;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SimilarityList {
	
	
	private List<NodeSim> similarityList; // = new ArrayList<NodeSim>();
	private  int maxListSize;
	
	public SimilarityList(int maxlength){
		similarityList = new ArrayList<NodeSim>();
		this.maxListSize = maxlength;
		
	}
	
	public boolean containsItem(NodeSim nodeSim){
		return this.similarityList.contains(nodeSim);
	}
	
	
	public void addItem(NodeSim nodeSim){
		if (nodeSim.nodeId <= 0) return;
		
		if(this.maxListSize<=0) return;
		int listSize = this.similarityList.size();
		
		if ( listSize<this.maxListSize)
		{
			this.similarityList.add(nodeSim);
			Comparator<NodeSim> r = Collections.reverseOrder();
			Collections.sort(this.similarityList, r);
			
		}
		else if(this.similarityList.get(listSize-1).similarity < nodeSim.similarity) {  
				this.similarityList.remove(listSize-1);
				this.similarityList.add(nodeSim);
				Comparator<NodeSim> r = Collections.reverseOrder();
				Collections.sort(this.similarityList, r);
			}
			
		return ;
		
	}
	
	
	public void updateSimilarity(int nodeId , float similarity){
		if (nodeId <= 0)
			return;
		
		NodeSim n = new NodeSim(nodeId , similarity);
		if (!this.similarityList.contains(n)){
			this.addItem(n);
		}
		else{
			this.removeItem(n.nodeId);
			this.addItem(n);
		}
	}
	
	public List<NodeSim> getCopyList(){
		List<NodeSim> result = new ArrayList<NodeSim>();
		for (NodeSim item : this.similarityList) {
			result.add(item);
			
		}
		return result;
	}
	
	
	public int getSize(){
		return Math.max(0, this.similarityList.size());
	}
	
	public void addItem(int peerId, float similarity){
		if (peerId <=0)
			return;
		NodeSim newItem = new NodeSim(peerId, similarity);
		if (this.containsItem(newItem)) {
			this.updateSimilarity(peerId, similarity);
			return;
		}
		
		this.addItem(newItem);
		
	}
	
	
	public List<NodeSim> getRandomItems(int numberOfRequestedItems){
		int listSize = this.getSize();
		int numberOfPossibleItems = Math.min(numberOfRequestedItems, listSize);
		if (numberOfRequestedItems > listSize){
			return this.similarityList;
			
		}
		
		List<NodeSim> tempList = this.getCopyList();
		Collections.shuffle(tempList);
		return tempList.subList(0, numberOfPossibleItems);
	}
	
	
	
	public List<NodeSim> getRandomItemsBiased(int numberOfRequestedItems){
		int listSize = this.getSize();
		int numberOfPossibleItems = Math.min(numberOfRequestedItems, listSize);
		if (numberOfRequestedItems > listSize){
			return this.similarityList;
		}
		
		
		List<NodeSim> tempList = this.getCopyList();
		List<NodeSim> l1 = tempList.subList(0, (int)listSize/4);
		List<NodeSim> l2 = tempList.subList((int)listSize/4, (int)listSize/2);
		List<NodeSim> l3 = tempList.subList((int)listSize/2, (int)3*listSize/4);
		List<NodeSim> l4 = tempList.subList((int)3*listSize/4, listSize);
		
		
		int diffTrans = 0;
		int n1 = Math.min((int) (numberOfPossibleItems * 0.45) , l1.size());
		diffTrans = (int) (numberOfPossibleItems * 0.45) - n1;
		int n2 = Math.min((int) (numberOfPossibleItems * 0.30)+(int)(diffTrans/3) , l2.size());
		diffTrans = (int) (numberOfPossibleItems * 0.75) - n1-n2;
		
		int n3 = Math.min((int) (numberOfPossibleItems * 0.15)+(int)(diffTrans/2) , l3.size());
		
		diffTrans = (int) (numberOfPossibleItems * 0.90) - n1-n2-n3;
		
		int n4 = Math.min((int) (numberOfPossibleItems * 0.10)+diffTrans , l4.size());
		if (n1+n2+n3+n4 < numberOfPossibleItems)
			n4 = numberOfPossibleItems - (n1+n2+n3);
		
		List<NodeSim> resulList = new ArrayList<NodeSim>();
		Collections.shuffle(l1);
		resulList.addAll(l1.subList(0	, n1));
		
		Collections.shuffle(l2);
		resulList.addAll(l2.subList(0	, n2));
		
		Collections.shuffle(l3);
		resulList.addAll(l3.subList(0	, n3));
		
		Collections.shuffle(l4);
		resulList.addAll(l4.subList(0	, n4));
		
		return resulList;
		

	}
	
	
	
	public void removeItem(NodeSim n){
		this.similarityList.remove(n);
	}
	
	public void removeItem(int nodeId){
		NodeSim n = new NodeSim(nodeId, 0);
		if (this.containsItem(n)){
			this.similarityList.remove(n);
			
		}
		
	}
	
	public boolean isFull(){
		return (this.maxListSize == this.similarityList.size());
	}
	
	
	public String toString(){
		return this.similarityList.toString();
	}
	
	public NodeSim getItem(int index){
		if (this.similarityList.isEmpty() || index >= this.similarityList.size() )
			return null;
		else 
			return this.similarityList.get(index);
	}
	
	
	public static void main(String args[]){
		SimilarityList l = new SimilarityList(3);
		
		Random r = new Random(147);
		for (int i = 1; i <= 3; i++) {
			float f = r.nextFloat();
			l.addItem(i,f);
		}
		
		log(l.toString());
		l.addItem(4,(float)0.75);
		log(l.toString());
		l.addItem(5,(float)0.76);
		
		FileOutputStream myFile;
		try {
			myFile = new FileOutputStream("src/peersim/simildis/result/similarity1.txt");
			PrintStream out = new PrintStream(myFile);
			out.println("#index|similarity");
			
			List<NodeSim> lll = l.getRandomItemsBiased(1);
			Collections.sort(lll);
			for (Iterator iterator = lll.iterator(); iterator.hasNext();) {
				NodeSim item = (NodeSim) iterator.next();
				out.println(item.nodeId+"|"+item.similarity);
				
			}
			try {
				myFile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log("List itself   :"+l.toString());
		List<NodeSim> ll = l.getRandomItemsBiased(1);
		try {
			myFile = new FileOutputStream("src/peersim/simildis/result/similarity_biased.txt");
			PrintStream out = new PrintStream(myFile);
			out.println("#index|similarity");
			Collections.sort(ll);
			for (Iterator iterator = ll.iterator(); iterator.hasNext();) {
				NodeSim item = (NodeSim) iterator.next();
				out.println(item.nodeId+"|"+item.similarity);
				
			}
			try {
				myFile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void log(String s){
		System.out.println(s);
	}
	
	
// An internal class 
class NodeSim implements Comparable<NodeSim>
{
    public int nodeId;
    public float similarity;
    
    public NodeSim(int nodeId, float similarity) {
        this.nodeId	= nodeId;
        this.similarity = similarity;
    }

	public int compareTo(NodeSim o2){
		
		if (this.similarity < o2.similarity)
			return -1;
		else if  (this.similarity == o2.similarity)
			return 0;
		else 
			return +1;
	}
				
	public String toString(){
		return "("+this.nodeId+","+this.similarity+")";
	}
	
	public boolean equals(Object o2){
		return this.nodeId==((NodeSim)o2).nodeId;
	}
}




}