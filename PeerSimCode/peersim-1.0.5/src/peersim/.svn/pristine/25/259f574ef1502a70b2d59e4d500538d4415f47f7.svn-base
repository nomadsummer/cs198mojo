package peersim.simildis;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.graph.NeighbourListGraph;
import peersim.simildis.MsgBuffer.MSG;
import peersim.simildis.SimilarityList.NodeSim;

public class SimilDis implements CDProtocol{
	
	protected int counter ;
	private final String prefix;
	
	protected static final String PAR_START_VAL = "start";
	protected static final String PAR_LIST_LENGTH = "sim_list_length";
	protected static final String PAR_BUFF_CAP ="buffer_capacity";
	protected static final String PAR_TIMES_TO_SEND ="num_times_to_send";
	protected static final String PAR_FAN_SIZE ="fan_size";
	
	protected SimilarityList similarityList;
	private int simMaxLength ; 
	
	//Subjective graph of the node 
	NeighbourListGraph subjectiveGraph;
	
	
	public Set<InfoEdge> newlyReceivedInfo ; 
	
	public SimilarityDAG similarityGraph;
	
	public MsgBuffer buffer;
	
	private final int buffer_capacity ;
	private final int num_times_to_send;
	private final int fan_size;

	public Object clone() {
		SimilDis pubsub = new SimilDis(this.prefix);
		return pubsub;
	}
	
	
	public SimilDis(String prefix){
		this.prefix = prefix;
		counter = (Configuration.getInt(prefix + "." + PAR_START_VAL, 1));
		simMaxLength  = (Configuration.getInt(prefix + "." + PAR_LIST_LENGTH, 1));
		this.similarityList = new SimilarityList(simMaxLength);
		this.subjectiveGraph = new NeighbourListGraph(true);
		this.similarityGraph = new SimilarityDAG();
		this.newlyReceivedInfo = new TreeSet<InfoEdge>();
		
		this.buffer_capacity = (Configuration.getInt(prefix + "." + PAR_BUFF_CAP, 1));
		this.num_times_to_send = (Configuration.getInt(prefix + "." + PAR_TIMES_TO_SEND, 1));
		this.fan_size = (Configuration.getInt(prefix + "." + PAR_FAN_SIZE, 1));
		
		this.buffer = new MsgBuffer(buffer_capacity, (byte)num_times_to_send);
		
 	}
	
	@Override	
	public void nextCycle(Node node, int protocolID) {
		// TODO Auto-generated method stub
		if (node.getIndex()==0)
			return;
		counter ++;
		int linkableID = FastConfig.getLinkable(protocolID);
		
        //Linkable linkable = (Linkable) node.getProtocol(linkableID);
        //long current_cycle = peersim.core.CommonState.getTime();
        //log("Cycle: "+current_cycle+"\tNode:"+ node.getIndex()+"\t\tHighest Similarr Node:"+ this.similarityList.getItem(0));
		
        List<NodeSim> targetNodes = this.similarityList.getRandomItems(this.fan_size);
		//List<NodeSim> targetNodes = this.similarityList.getRandomItemsBiased(this.fan_size);
        
		List<MSG> freshRecs = this.buffer.getFreshRecords(1);
        
        if (freshRecs != null && targetNodes != null)
        	if (freshRecs.size() >0 && targetNodes.size() > 0)
        		{
        			//TEST 
        		    //doTransfer(targetNodes,freshRecs, node.getIndex());
        			//updateAges(freshRecs);
        			
        		}
        
    }
	
	public void doTransfer(List<NodeSim> targetNodes , List<MSG> freshRecs, int sender){
		for (Iterator iterator = targetNodes.iterator(); iterator.hasNext();) {
			NodeSim ns = (NodeSim) iterator.next();
			
			//log("node id to send:"+ ns.nodeId);
			//if (ns.nodeId==3)
			//	log("Node:"+ sender+" Sending to:"+ns.toString());
			SimilDis p = (SimilDis) Network.get(ns.nodeId).getProtocol(1);
			for (MSG msg : freshRecs) {
				//MSG msg = (MSG) iterator2.next();
				p.newlyReceivedInfo.add(new InfoEdge(msg.peer_id_from,msg.peer_id_to,this.subjectiveGraph.getEdgeWeight(msg.peer_id_from,msg.peer_id_to)));
			}
			
		}
	}
	
	public void updateAges( List<MSG> sentRecs){
		for (Iterator iterator2 = sentRecs.iterator(); iterator2.hasNext();) {
			MSG msg = (MSG) iterator2.next();
			this.buffer.increaseAge(msg);
			
		}
	}
	
	private void log(String msg){

		System.out.println(msg);
		
		
	}
	
	
	

}
