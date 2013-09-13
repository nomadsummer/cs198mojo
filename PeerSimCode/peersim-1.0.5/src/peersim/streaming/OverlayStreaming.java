/*
 * Copyright (c) 2003 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package peersim.streaming;

import peersim.config.*;
import peersim.core.*;
import peersim.transport.Transport;
import peersim.cdsim.CDProtocol;
import peersim.edsim.*;
import java.lang.Double;
import java.util.*;

/**
* Event driven version of epidemic averaging.
*/
public class OverlayStreaming implements CDProtocol, EDProtocol {

//--------------------------------------------------------------------------
// Initialization
//--------------------------------------------------------------------------

/**
 * @param prefix string prefix for config properties
 */
public OverlayStreaming(String prefix) {
}

//--------------------------------------------------------------------------
//Fields
//--------------------------------------------------------------------------

/* these constant are equal for all the nodes and fixed */
protected static final int EV_GENERATION    = 0;
protected static final int EV_UPDATE        = 1;
protected static final int EV_LEAVE         = 2;
protected static final int JOIN             = 3;
protected static final int JOIN_REPLYYES    = 4;
protected static final int JOIN_REPLYNO     = 5;
protected static final int CONNECT          = 6;
protected static final int SET_DELAY        = 7;
protected static final int RESET_RESERVE    = 8;
protected static final int LEAVING          = 9;
protected static final int SWITCH_STATUS    = 10;
protected static final int SWITCH_REPLYYES  = 11;
protected static final int SWITCH_REPLYNO   = 12;
protected static final int STRIPE_LEAVING   = 13;
protected static final int CHANGE_STRIPE    = 14;
protected static final int CHANGE_STRIPE_OK = 15;
protected static final int REMOVE_CHILD     = 16;


protected static final int ACTIVE        = 111;
protected static final int STANDBY       = 100;


protected static final int CHILD         = 123;
protected static final int PARENT        = 321;

protected static final int HOWMANYREQ    = 10;

/* these constant are equal for all the nodes and initialized at the beginning */
protected static int    TOTAL_NODES      = 100;
protected static int    INIT_NODES       = 50;
protected static int    NUM_STRIPES      = 10;
protected static int    ACTIVE_STRIPES   = 4;
protected static double P_CHURN          = 1.0;

protected static long   MAX_TIME         = CommonState.getEndTime();

protected static int    MAXCHILDREN      = 14;
protected static double STREAMING_RATE   = 0.1;

protected static long   TIME_OTHER_NODES = (long)(0.02*MAX_TIME);
protected static double STRIPE_RATE      = STREAMING_RATE/((double)ACTIVE_STRIPES);
protected static double LEAVING_RATE     = (1.0)/(P_CHURN*(double)MAX_TIME);

protected static double ARRIVAL_RATE     = ((double)(TOTAL_NODES-INIT_NODES)/(double)TIME_OTHER_NODES) + ((double)INIT_NODES*LEAVING_RATE) + ((double)(INIT_NODES-NUM_STRIPES)/((double)MAX_TIME/100.0));

private static final double[] inputBandwidth = {0.1, 0.2, 0.5};

//private static final double[] inputProbab    = {0.2 , 0.4, 0.4};
//you need the CDF
private static final double[] inputProbab    = {0.2 , 0.6, 1.0};

protected static boolean observerSet         = false;
protected static Vector timeObserved         = new Vector();
protected static Vector parentsObserved      = new Vector();

/** Value held by this protocol */
protected  boolean online;      /* the node is/is not downloading */
protected  double  bw;           /* the upload bandwidth of the node */
protected  double  usedBw;       /* the actual (upload) bandwidth used to transfer chunks */

protected  double  delay;        /* delay from the source */
protected  int     numStripes;   /* received standby stripes */
protected  int     numActStripes;/* received active stripes */
protected  int     stripesId;    /* received standby stripes */
protected  int     actStripesId; /* received active stripes */
protected  int     numChildren;  /* number of children the node has */
protected  int     numTmpChildren;  /* number of temporary accepted children the node has */
protected  int     numParents;   /* number of parents the node has */
protected  long    leaveTime;    /* time when the node will leave the network */

protected  boolean orphanOfActive, observer;

protected  int    receivedReplies, sentRequests, sentSwitchRequests, receivedSwitchReplies;

private   Node[]    childrenId      = new   Node[20]; /* pointers to children */
private    int[]    childrenStripe  = new    int[20];
private    int[]    childrenType    = new    int[20];
private   Node[]    parentsId       = new   Node[20]; /* pointers to parents */
private    int[]    parentsStripe   = new    int[20];
private double[]    parentsDelay    = new double[20];

//--------------------------------------------------------------------------
// methods
//--------------------------------------------------------------------------


/**
 * Clones the value holder.
 */
public Object clone()
{
	OverlayStreaming osh = null;
	try { osh = (OverlayStreaming)super.clone(); }
	catch( CloneNotSupportedException e ) {} // never happens
	osh.childrenId     = new Node[childrenId.length];
	osh.childrenStripe = new  int[childrenStripe.length];
	osh.childrenType   = new  int[childrenType.length];
	osh.parentsId       = new Node[parentsId.length];
	osh.parentsStripe   = new  int[parentsStripe.length];
	osh.parentsDelay    = new double[parentsDelay.length];
	System.arraycopy(childrenId,    0, osh.childrenId,    0, childrenId.length);
	System.arraycopy(childrenStripe,0, osh.childrenStripe,0, childrenStripe.length);
	System.arraycopy(childrenType,  0, osh.childrenType,  0, childrenType.length);
	System.arraycopy(parentsId,      0, osh.parentsId,      0, parentsId.length);
	System.arraycopy(parentsStripe,  0, osh.parentsStripe,  0, parentsStripe.length);
	System.arraycopy(parentsDelay,   0, osh.parentsDelay,   0, parentsDelay.length);
	return osh;
}

public int getOnline()
{
	if (online) return(1);
	return (0);
}

public double getBw()
{
	return bw;
}

public double getUsedBw()
{
	return usedBw;
}

public double getSpareBw()
{
	return (bw-usedBw);
}

public double getDelay()
{
	return delay;
}

public int getNumStripes()
{
	return numStripes;
}

public int getNumActStripes()
{
	return numActStripes;
}

public int getStripesId()
{
	return stripesId;
}

public int getActStripesId()
{
	return actStripesId;
}

public int getChildren()
{
	return numChildren;
}

public int getNumActChildren()
{
	int output = 0;

	for (int k = 0; k < numChildren; k++) {
		if(childrenType[k]==ACTIVE)
			output++;
	}
	return output;
}

public int getTmpChildren()
{
	return numTmpChildren;
}

public int getParents()
{
	return numParents;
}

public boolean getObserver()
{
	return observer;
}

public Vector getTimeObserved()
{
	return timeObserved;
}

public Vector getParentsObserved()
{
	return parentsObserved;
}


//--------------------------------------------------------------------------

/**
 * @inheritDoc
 */

public void resetAll()
{
	this.online         = true;
	this.bw             = 0;
	this.usedBw         = 0;
	this.delay          = 0;
        this.numStripes     = 0;
        this.numActStripes  = 0;
        this.stripesId      = 0;
        this.actStripesId   = 0;
	this.numChildren    = 0;
	this.numTmpChildren = 0;
	this.numParents     = 0;
	this.leaveTime      = 0;

	this.observer        = false;
	this.orphanOfActive = false;
	this.receivedReplies = 0;
	this.sentRequests = 0;
	this.receivedSwitchReplies = 0;
	this.sentSwitchRequests = 0;

}
 

public void setTotalNodes(int numNodes)
{
	this.TOTAL_NODES = numNodes;
}

public void setInitNodes(int numNodes)
{
	this.INIT_NODES = numNodes;
}

public void setGlobalStripes(int numStripes)
{
	this.NUM_STRIPES = numStripes;
}

public void setActiveStripes(int numStripes)
{
	this.ACTIVE_STRIPES = numStripes;
}

public void setPChurn(double prob)
{
	this.P_CHURN = prob;
}

public void setOtherParameters()
{
	this.MAX_TIME = CommonState.getEndTime();
	this.MAXCHILDREN      = 14;
	this.STREAMING_RATE   = 0.1;
	this.TIME_OTHER_NODES = (long)(0.02*MAX_TIME);
	this.STRIPE_RATE      = STREAMING_RATE/((double)ACTIVE_STRIPES);
	this.LEAVING_RATE     = (1.0)/(P_CHURN*(double)MAX_TIME);
	this.ARRIVAL_RATE     = ((double)(TOTAL_NODES-INIT_NODES)/(double)TIME_OTHER_NODES) + ((double)INIT_NODES*LEAVING_RATE) + ((double)(INIT_NODES-NUM_STRIPES)/((double)MAX_TIME/100.0));

        this.observerSet    = false;
	this.timeObserved.clear();
	this.parentsObserved.clear(); 

}


public void setOnline(boolean status)
{
	this.online = status;
}

public void setBw(double bw)
{
	this.bw = bw;
}

public void setUsedBw(double usedBw)
{
	this.usedBw = usedBw;
}

public void setDelay(int delay)
{
	this.delay = delay;
}

public void setNumStripes(int stripe)
{
	this.numStripes = stripe;
}

public void setNumActStripes(int stripe)
{
	this.numActStripes = stripe;
}

public void setStripesId(int id)
{
	this.stripesId = id;
}

public void setActStripesId(int id)
{
	this.actStripesId = id;
}

public void setChildren(int children)
{
	this.numChildren = children;
}

public void setParents(int parents)
{
	this.numParents = parents;
}

public void setParentValues(int parentIndex, int stripeId, Node nodeId, double parentDelay)
{
	parentsStripe[parentIndex] = stripeId;
	parentsId[parentIndex]     = nodeId;
	parentsDelay[parentIndex]  = parentDelay;
}


public void setLeaveTime(long time)
{
	this.leaveTime = time;
}

//--------------------------------------------------------------------------
public void nextCycle( Node node, int pid )
{
}

/**
* This is the standard method to define to process incoming messages.
*/
public void processEvent( Node node, int pid, Object event ) {
		
	Linkable linkable;
	Node peern;
	long newTime;
	int availableStripe, status, i, j, rndInt, oldStripe;
	double stripeDelay;
	boolean found;

	ArrivedMessage aem = (ArrivedMessage)event;
	
	switch(aem.typeOfMsg) {
		case EV_GENERATION:
		LEAVING_RATE     = (1.0)/(P_CHURN*MAX_TIME);
		ARRIVAL_RATE     = ( (double)(TOTAL_NODES-INIT_NODES)/ (double)TIME_OTHER_NODES ) + 
					(((double)INIT_NODES)*LEAVING_RATE) + 
					((double)(INIT_NODES-NUM_STRIPES))/(((double)MAX_TIME)/100.0);
		/* a brand new node arrives in the network */
			resetAll(); /* in resetting, the 'online' status changes to 'true' */
			delay = -1; /* identify the node that just entered in the network */
			rndInt = CommonState.r.nextInt(1000);
			setBw(inputBandwidth[0]);
			for (j = 0; j < inputProbab.length-1; j++){
				if (rndInt > 1000*inputProbab[j]){
					setBw(inputBandwidth[j+1]);
				}//if
			}//for j
			/* generate the next brand new node */
			Node newNode = new GeneralNode("");
			OverlayStreaming prot = (OverlayStreaming) newNode.getProtocol(pid);
			prot.setOnline(false);
			prot.setDelay(-1); /* identify the node that just entered in the network */
			Network.add(newNode);
			if ((CommonState.getTime()< TIME_OTHER_NODES)&&
				(CommonState.getTime()> (MAX_TIME/100))) {
				ARRIVAL_RATE = ((double)(TOTAL_NODES-INIT_NODES)/(double)TIME_OTHER_NODES) + 
						((double)INIT_NODES*LEAVING_RATE) +
						((double)(TOTAL_NODES-INIT_NODES)*CommonState.getTime()*LEAVING_RATE/(double)TIME_OTHER_NODES);
			}
			else if (CommonState.getTime()> TIME_OTHER_NODES){
				ARRIVAL_RATE = (double)TOTAL_NODES*LEAVING_RATE;
			}
			newTime = (long)Math.max(1.0, CommonState.r.nextLong((long)(Math.max(1,(2.0/ARRIVAL_RATE)))) );
//			newTime = (long)Math.max(1.0, nextNegExp(1.0/ARRIVAL_RATE) );
			EDSimulator.add(newTime, new ArrivedMessage(EV_GENERATION, newNode, 0, 0), newNode, pid);
			/* start sending JOIN messages */
			Network.shuffle();
			sentRequests = Math.min(HOWMANYREQ*NUM_STRIPES, Network.size());
			int sentRequestsAdjust = 0;
			for (int k = 0; k < sentRequests; k++) {
				peern = Network.get(k);
				if (peern != node){
					((Transport)node.getProtocol(FastConfig.getTransport(pid))).
						send(
							node,
							peern,
							new ArrivedMessage(JOIN, node, 0, 0),
							pid);
				}
				else {sentRequestsAdjust++;}
			}
			sentRequests -= sentRequestsAdjust;
			/* decide when UPDATE and LEAVE */
			newTime = CommonState.r.nextLong(2*TIME_OTHER_NODES);
//			newTime = (long)nextNegExp((double)TIME_OTHER_NODES);
			EDSimulator.add(newTime, new ArrivedMessage(EV_UPDATE, node, 0, 0), node, pid);
			newTime = CommonState.r.nextLong((long)(2.0/LEAVING_RATE));
//			newTime = (long)nextNegExp(1.0/LEAVING_RATE);
			EDSimulator.add(newTime, new ArrivedMessage(EV_LEAVE, node, 0, 0), node, pid);
			leaveTime = CommonState.getTime() + newTime;
			if ((Network.size() > (TOTAL_NODES/2))&&
				(leaveTime>CommonState.getEndTime())&&
				(!observerSet)) {
				observerSet = true;
				observer    = true;
			}
			if(observer){
				timeObserved.add(CommonState.getTime());
				parentsObserved.add(numParents);
				i = parentsObserved.size()-1;
				if ((i>2)&&(parentsObserved.elementAt(i) == parentsObserved.elementAt(i-1))){
					timeObserved.removeElementAt(i); 
					parentsObserved.removeElementAt(i); 
				}
			}
			break;
		case JOIN:
		/* a request of join arrived from a node; reply 'ok' if you are up and have
		   bandwidth, reply 'no' otherwise */
			availableStripe = aem.stripeId^(aem.stripeId|actStripesId);
			if ((online)&&(bw-usedBw>=STRIPE_RATE)&&
			    (availableStripe>0)&&(numTmpChildren < MAXCHILDREN)){
				/*reserve bw and number of children and reply */
				usedBw += STRIPE_RATE;
				numTmpChildren++;
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).
					send(
						node,
						aem.sender,
						new ArrivedMessage(JOIN_REPLYYES, node, actStripesId, 0),
						pid);
			}
			else {
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).
					send(
						node,
						aem.sender,
						new ArrivedMessage(JOIN_REPLYNO, node, 0, 0),
						pid);
			}
			break;
		case JOIN_REPLYYES:
		/* one of my neighbor can provide me a stripe */
			receivedReplies++;
			availableStripe = stripesId + actStripesId;
			availableStripe = availableStripe^(availableStripe|aem.stripeId);
			if (availableStripe >= Math.pow(2, NUM_STRIPES))
				availableStripe = 0;
			if ((online)&&(availableStripe>0)){
				/*select a stripe and connect to it */
				availableStripe = chooseRndStripe(availableStripe);
				if (numActStripes < ACTIVE_STRIPES){
					numActStripes++;
					actStripesId += availableStripe;
					status = ACTIVE;
				}
				else {
					numStripes++;
					stripesId += availableStripe;
					status = STANDBY;
				}
				parentsStripe[numParents] = availableStripe;
				parentsId[numParents] = aem.sender;
				numParents++;
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).
					send(
						node,
						aem.sender,
						new ArrivedMessage(CONNECT, node, availableStripe, status),
						pid);
			}
			else {
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).
					send(
						node,
						aem.sender,
						new ArrivedMessage(RESET_RESERVE, node, 0, 1), // msgParameter = 1: see the RESET_RESERVE event
						pid);
			}
			if(observer){
				timeObserved.add(CommonState.getTime());
				parentsObserved.add(numParents);
				i = parentsObserved.size()-1;
				if ((i>2)&&(parentsObserved.elementAt(i) == parentsObserved.elementAt(i-1))){
					timeObserved.removeElementAt(i); 
					parentsObserved.removeElementAt(i); 
				}
			}
			break;
		case JOIN_REPLYNO:
		/* one of my neighbor cannot provide me a stripe */
			receivedReplies++;
			if ((online)&&(receivedReplies == sentRequests)&&(numParents == 0)){
				/* do something, such as send other requests, or set an error state 
				   and leave the network */
			}
			break;
		case CONNECT:
		/* my new children inform me that now I am its parent */
			availableStripe = aem.stripeId&actStripesId;
			if ((online)&&(availableStripe > 0)){
				if(aem.msgParameter == STANDBY){
					usedBw -= STRIPE_RATE;
				}
				childrenId[numChildren] = aem.sender;
				childrenStripe[numChildren] = aem.stripeId;
				childrenType[numChildren] = aem.msgParameter;
				numChildren++;
				stripeDelay = 0;
				for (i = 0; i < numParents; i++){
					if (parentsStripe[i] == aem.stripeId) {
						stripeDelay = parentsDelay[i] + 1.0/(ACTIVE_STRIPES*STRIPE_RATE);
						break; 
					}
				}//for i
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).
					send(
						node,
						aem.sender,
						new ArrivedMessage(SET_DELAY, node, aem.stripeId, (int)stripeDelay),
						pid);
			}
			else { /* the node went down in the meantime or has no more the stripe */
				if(aem.msgParameter == ACTIVE){
					usedBw -= STRIPE_RATE;
				}
				numTmpChildren--;
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).
					send(
						node,
						aem.sender,
						new ArrivedMessage(LEAVING, node, 0, PARENT),
						pid);
			}
			break;
		case SET_DELAY:
		/* my parent inform me about the value of the delay for the stripe it is giving*/
			if (online){
				found = false;
				for (i = 0; i < numParents; i++){
					if (parentsStripe[i] == aem.stripeId) {
						parentsDelay[i] =  (double)aem.msgParameter;
						found = true;
						break; 
					}
				}//for i
				if ((found)&&((aem.stripeId&actStripesId)>0)){ /*it is an active parent*/
					delay = Math.max(delay, (double)aem.msgParameter);
				}
			}
			if(observer){
				timeObserved.add(CommonState.getTime());
				parentsObserved.add(numParents);
				i = parentsObserved.size()-1;
				if ((i>2)&&(parentsObserved.elementAt(i) == parentsObserved.elementAt(i-1))){
					timeObserved.removeElementAt(i); 
					parentsObserved.removeElementAt(i); 
				}
			}
			break;
		case RESET_RESERVE:
		/* remove a reservation, since the stripe is no more necessary */
			if (online){
				usedBw -= STRIPE_RATE;
				numTmpChildren -= aem.msgParameter;
				/* since this event is used in different situations, we use msgParameter
				   to distinguish when it is used */ 
				for (int k = 0; k < numChildren; k++) {
					peern = childrenId[k];
					if (peern == aem.sender)
						childrenType[k] = STANDBY;
				}
			}
			break;
		case EV_UPDATE:
		/* periodical update of the indegree */
			if (online){
				/* reschedule an EV_UPDATE */
				newTime = CommonState.r.nextLong(2*TIME_OTHER_NODES);
//				newTime = (long)nextNegExp((double)TIME_OTHER_NODES);
				EDSimulator.add(newTime, new ArrivedMessage(EV_UPDATE, node, 0, 0), node, pid);
			}
			if ((online)&&(numParents < NUM_STRIPES)){
				/* send out requests */
				receivedReplies = 0;
				availableStripe = stripesId + actStripesId;
				sentRequests = Math.min(HOWMANYREQ*NUM_STRIPES, Network.size());
				sentRequestsAdjust = 0;
				Network.shuffle();
				for (int k = 0; k < sentRequests; k++) {
					peern = Network.get(k);
					found = false;
					for (i = 0; i < numParents; i++) {
					/* check if the node is already your parent */
						if (peern == parentsId[i])
							found = true;
					}
					if ((peern != node)&&(!found)){
						((Transport)node.getProtocol(FastConfig.getTransport(pid))).
							send(
								node,
								peern,
								new ArrivedMessage(JOIN, node, availableStripe, 0),
								pid);
					}
					else {sentRequestsAdjust++;}
				}
				sentRequests -= sentRequestsAdjust;
			}
			break;
		case EV_LEAVE:
		/* node leaves the network: send a message to all parents/children*/
			if (online){
				online = false;
				for (int k = 0; k < numParents; k++) {
					peern = parentsId[k];
					((Transport)node.getProtocol(FastConfig.getTransport(pid))).
						send(
							node,
							peern,
							new ArrivedMessage(LEAVING, node, 0, CHILD),
							pid);
				}
				for (int k = 0; k < numChildren; k++) {
					peern = childrenId[k];
					((Transport)node.getProtocol(FastConfig.getTransport(pid))).
						send(
							node,
							peern,
							new ArrivedMessage(LEAVING, node, 0, PARENT),
							pid);
				}
			}
			break;
		case LEAVING:
		/* one of my children or parents has left */
			if (online){
				if (aem.msgParameter == CHILD){ //a child left
					numTmpChildren--;
					found = false;
					for (int k = 0; k < numChildren; k++) {
						peern = childrenId[k];
						if (peern == aem.sender){
							swapNodes(childrenId,     k, numChildren-1);
							  swapInt(childrenStripe, k, numChildren-1);
							  swapInt(childrenType,   k, numChildren-1);
							numChildren--;
							if (childrenType[numChildren]==ACTIVE)
								usedBw -= STRIPE_RATE;
							found = true;
							break;
						}
					}
				}//if (aem.msgParameter == CHILD)
				if ((aem.msgParameter == PARENT)){ // a parent left
				    found = false;
				    for (int k = 0; k < numParents; k++) {
				    	peern = parentsId[k];
				    	if (peern == aem.sender){
				    		swapNodes(parentsId,     k, numParents-1);
				    		  swapInt(parentsStripe, k, numParents-1);
				    	       swapDouble(parentsDelay,  k, numParents-1);
				    		numParents--;
				    		found = true;
				    		break;
				    	}
				    }
				    if (found) {
					availableStripe = parentsStripe[numParents];
					orphanOfActive = false;
					if ((availableStripe&actStripesId)>0)
						orphanOfActive = true;
					if (!orphanOfActive){ // was a standby parent
						stripesId -= parentsStripe[numParents];
						numStripes--;
					}
					else { // search standby parents
						actStripesId -= parentsStripe[numParents];
						numActStripes--;
						// you should change the delay
						delay = 0;
						sentSwitchRequests = 0;
						for (int k = 0; k < numParents; k++) {
							availableStripe = parentsStripe[k];
							if ((availableStripe&stripesId)>0){
								peern = parentsId[k];
								((Transport)node.getProtocol(FastConfig.getTransport(pid))).
									send(
										node,
										peern,
										new ArrivedMessage(SWITCH_STATUS, node, parentsStripe[k], 0),
										pid);
								sentSwitchRequests++;
							}
							else {// it's an active parent, update the delay
								delay = Math.max(delay, parentsDelay[k]);
							}
						}
						// inform your children that are downloading the stripe
						for (int k = 0; k < numChildren; k++) {
							availableStripe = childrenStripe[k];
							if (availableStripe==parentsStripe[numParents]){
								peern = childrenId[k];
								((Transport)node.getProtocol(FastConfig.getTransport(pid))).
									send(
										node,
										peern,
										new ArrivedMessage(STRIPE_LEAVING, node, parentsStripe[numParents], actStripesId),
										pid);
							}
						}
					}//if (orphanOfActive){
				    } //if(found
				}//if (aem.msgParameter == PARENT)
			}//if (online)
			if(observer){
				timeObserved.add(CommonState.getTime());
				parentsObserved.add(numParents);
				i = parentsObserved.size()-1;
				if ((i>2)&&(parentsObserved.elementAt(i) == parentsObserved.elementAt(i-1))){
					timeObserved.removeElementAt(i); 
					parentsObserved.removeElementAt(i); 
				}
			}
			break;
		case SWITCH_STATUS:
		/* one of my children is asking to switch from STANDBY to ACTIVE */
			availableStripe = aem.stripeId&actStripesId;
			if ((online)&&(availableStripe > 0)&&(bw-usedBw >= STRIPE_RATE)){
				usedBw += STRIPE_RATE;
				for (int k = 0; k < numChildren; k++) {
					peern = childrenId[k];
					if (peern == aem.sender)
						childrenType[k] = ACTIVE;
				}
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).
					send(
						node,
						aem.sender,
						new ArrivedMessage(SWITCH_REPLYYES, node, aem.stripeId, 0),
						pid);
			}
			else { /* the node went down in the meantime or has no more the stripe or has no spare bw */
				found = false;
				for (int k = 0; k < numChildren; k++) {
					peern = childrenId[k];
					if (peern == aem.sender){
						swapNodes(childrenId,     k, numChildren-1);
						  swapInt(childrenStripe, k, numChildren-1);
						  swapInt(childrenType,   k, numChildren-1);
						numChildren--;
						numTmpChildren--;
						found = true;
						break;
					}
				}
				if (found) {
					((Transport)node.getProtocol(FastConfig.getTransport(pid))).
						send(
						node,
						aem.sender,
						new ArrivedMessage(SWITCH_REPLYNO, node, 0, 0),
						pid);
				}
			}
			break;
		case SWITCH_REPLYYES:
		/* one of my parents replied 'YES' to a 'switch' request */
			receivedSwitchReplies++;
			if ((online)&&(orphanOfActive)){
				stripesId -= aem.stripeId;
				numStripes--;
				actStripesId += aem.stripeId;
				numActStripes++;
				found = false;
				for (int k = 0; k < numParents; k++) {
					peern = parentsId[k];
					if (peern == aem.sender){
						delay = Math.max(delay, parentsDelay[k]);
						found = true;
						break;
					}
				}
				if (numActStripes >= ACTIVE_STRIPES)
					orphanOfActive = false;
			}
			else { //the contribute of this parent is no more necessary
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).
					send(
						node,
						aem.sender,
						new ArrivedMessage(RESET_RESERVE, node, 0, 0), // msgParameter = 1: see the RESET_RESERVE event
						pid);
			}
			if(observer){
				timeObserved.add(CommonState.getTime());
				parentsObserved.add(numParents);
				i = parentsObserved.size()-1;
				if ((i>2)&&(parentsObserved.elementAt(i) == parentsObserved.elementAt(i-1))){
					timeObserved.removeElementAt(i); 
					parentsObserved.removeElementAt(i); 
				}
			}
			break;
		case SWITCH_REPLYNO:
		/* one of my parents replied 'NO' to a 'switch' request: remove the entry */
			receivedSwitchReplies++;
			found = false;
			for (int k = 0; k < numParents; k++) {
				peern = parentsId[k];
				if (peern == aem.sender){
					swapNodes(parentsId,     k, numParents-1);
					  swapInt(parentsStripe, k, numParents-1);
				       swapDouble(parentsDelay,  k, numParents-1);
					numParents--;
					found = true;
					break;
				}
			}
			if (found) {
				availableStripe = parentsStripe[numParents];
				if ((availableStripe&actStripesId)>0){ // should never happen
					actStripesId -= parentsStripe[numParents];
					numActStripes--;
				}
				else if ((availableStripe&stripesId)>0){ 
					stripesId -= parentsStripe[numParents];
					numStripes--;
				}
			}
			if ((online)&&(receivedSwitchReplies == sentSwitchRequests)&&(orphanOfActive)){
				/* in this case we have receive a 'NO' to the last switch request, and we
				   are still orphan of an active parent */
				if (numParents == 0){ //leave with an error
					EDSimulator.add(0, new ArrivedMessage(EV_LEAVE, node, 0, 0), node, pid);
				}
				else { // here you can choose to launche an UPDATE event; 
				       // since the simulator does not behave like this, do nothing
				}
			}
			if(observer){
				timeObserved.add(CommonState.getTime());
				parentsObserved.add(numParents);
				i = parentsObserved.size()-1;
				if ((i>2)&&(parentsObserved.elementAt(i) == parentsObserved.elementAt(i-1))){
					timeObserved.removeElementAt(i); 
					parentsObserved.removeElementAt(i); 
				}
			}
			break;
		case STRIPE_LEAVING:
		/* my parent is still present, but the stripe it is giving is not */
			availableStripe = actStripesId + stripesId;
			availableStripe -= aem.stripeId;
			availableStripe = availableStripe^(availableStripe|aem.msgParameter);
			if ((online)&&(availableStripe>0)){ //my parent has an alternative stripe
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).
					send(
						node,
						aem.sender,
						new ArrivedMessage(CHANGE_STRIPE, node, aem.stripeId, availableStripe), 
						pid);
			}
			else { //my parent hasn't any alternative stripe, send a 'remove' message
				status = STANDBY;
				if ((aem.stripeId&actStripesId)>0){
					status = ACTIVE;
				}
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).
					send(
						node,
						aem.sender,
						new ArrivedMessage(REMOVE_CHILD, node, 0, status),
						pid);
			}
			break;
		case CHANGE_STRIPE:
		/* my children ask me to change stripe */
			availableStripe = aem.msgParameter&actStripesId;
			if (availableStripe >= Math.pow(2, NUM_STRIPES))
				availableStripe = 0;
			if ((online)&&(availableStripe>0)){ //I still have the requested stripe
				availableStripe = chooseRndStripe(availableStripe);
				found = false;
				for (int k = 0; k < numChildren; k++) {
					peern = childrenId[k];	
					if (peern == aem.sender){
						childrenStripe[k] = availableStripe;
						found = true;
						break;
					}
				}
				if (found) {
					stripeDelay = 0;
					for (i = 0; i < numParents; i++){
						if (parentsStripe[i] == availableStripe) {
							stripeDelay = parentsDelay[i] + 1.0/(ACTIVE_STRIPES*STRIPE_RATE);
							break; 
						}
					}//for i
					((Transport)node.getProtocol(FastConfig.getTransport(pid))).
						send(
							node,
							aem.sender,
							new ArrivedMessage(CHANGE_STRIPE_OK, node, availableStripe, (int)stripeDelay),
							pid);
				}
			}
			else { //I have no more the alternative stripe, behave as if it is leaving
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).
					send(
						node,
						aem.sender,
						new ArrivedMessage(LEAVING, node, 0, PARENT),
						pid);
				// remove the node from the children list
				found = false;
				for (int k = 0; k < numChildren; k++) {
					peern = childrenId[k];
					if (peern == aem.sender){
						swapNodes(childrenId,     k, numChildren-1);
						  swapInt(childrenStripe, k, numChildren-1);
						  swapInt(childrenType,   k, numChildren-1);
						numChildren--;
						numTmpChildren--;
						found = true;
						if (childrenType[numChildren]==ACTIVE)
							usedBw -= STRIPE_RATE;
						break;
					}
				}
			}
			break;
		case CHANGE_STRIPE_OK:
		/* my parent changed the stripe */
			availableStripe = actStripesId + stripesId;
			availableStripe = availableStripe^(availableStripe|aem.stripeId);
			if ((online)&&(availableStripe>0)){ //I can change the stripe
				oldStripe = 0;
				found = false;
				for (i = 0; i < numParents; i++) {
					peern = parentsId[i];
					if (peern == aem.sender){
						oldStripe = parentsStripe[i];
						parentsStripe[i] = availableStripe;
						parentsDelay[i] = aem.msgParameter;
						found = true;
						break;
					}
				}
				if ((found)&(oldStripe&actStripesId)>0) {
					actStripesId -= oldStripe;
					actStripesId += availableStripe;
				}
				else if ((found)&(oldStripe&stripesId)>0) {
					stripesId -= oldStripe;
					stripesId += availableStripe;
				}
				else {
					found = false;
				}
				/* update the delay */
				delay = 0;
				for (j = 0; j < numParents; j++) {
					if ((parentsStripe[j]&actStripesId)>0){
						delay = Math.max(delay, parentsDelay[j]);
					}
				}
				/* after succesfully change of stripe, inform your children that
				   the previous stripe was no more available */
				if (!found)
					oldStripe = 0;
				for (int k = 0; k < numChildren; k++) {
					if (childrenStripe[k] == oldStripe){
						peern = childrenId[k];
						((Transport)node.getProtocol(FastConfig.getTransport(pid))).
							send(
								node,
								peern,
								new ArrivedMessage(STRIPE_LEAVING, node, oldStripe, actStripesId),
								pid);
					}
				}
			}
			else { //I cannot change the stripe
				found = false;
				oldStripe = 0;
				for (i = 0; i < numParents; i++) {
					peern = parentsId[i];
					if (peern == aem.sender){
						oldStripe = parentsStripe[i];
						found = true;
						break;
					}
				}
				if (found) {
					status = STANDBY;
					if ((oldStripe&actStripesId)>0){
						status = ACTIVE;
					}
					((Transport)node.getProtocol(FastConfig.getTransport(pid))).
						send(
							node,
							aem.sender,
							new ArrivedMessage(REMOVE_CHILD, node, 0, status),
							pid);
				}
			}
			if(observer){
				timeObserved.add(CommonState.getTime());
				parentsObserved.add(numParents);
				i = parentsObserved.size()-1;
				if ((i>2)&&(parentsObserved.elementAt(i) == parentsObserved.elementAt(i-1))){
					timeObserved.removeElementAt(i); 
					parentsObserved.removeElementAt(i); 
				}
			}
			break;
		case REMOVE_CHILD:
		/* my children cannot exploit my stripes: remove it from my list */
			((Transport)node.getProtocol(FastConfig.getTransport(pid))).
				send(
					node,
					aem.sender,
					new ArrivedMessage(LEAVING, node, 0, PARENT),
					pid);
			if (online){
				// remove the node from the children list
				numTmpChildren--;
				found = false;
				for (int k = 0; k < numChildren; k++) {
					peern = childrenId[k];
					if (peern == aem.sender){
						swapNodes(childrenId,     k, numChildren-1);
						  swapInt(childrenStripe, k, numChildren-1);
						  swapInt(childrenType,   k, numChildren-1);
						numChildren--;
						found = true;
						if (childrenType[numChildren]==ACTIVE)
							usedBw -= STRIPE_RATE;
						break;
					}
				}
			}
			break;
	}//switch(aem.typeOfMsg)

}



// ====================== helper methods ==============================
// ====================================================================
private int chooseRndStripe(int inputStripe) {

int numStripes, rndInt, j, outputStripe;    
            
    numStripes = 0;
    for (j=0; j < NUM_STRIPES; j++){
          if ((inputStripe&((int)(Math.pow(2,j)))) > 0)
              numStripes++;
         }
    rndInt = CommonState.r.nextInt(numStripes);
    j=0;
    while(rndInt>=0){
          if ((inputStripe&((int)(Math.pow(2,j)))) > 0)
               rndInt--;
               j++;
          }
    outputStripe = (int)Math.pow(2,j-1);
   
   return(outputStripe);
}   

// ======================================================

private void swapNodes(Node[] list, int i, int j) {
	
	Node n = list[i];
	list[i] = list[j];
	list[j] = n;
}

// ======================================================

private void swapInt(int[] list, int i, int j) {
	
	int n = list[i];
	list[i] = list[j];
	list[j] = n;
}

// ======================================================

private void swapDouble(double[] list, int i, int j) {
	
	double n = list[i];
	list[i] = list[j];
	list[j] = n;
}

// ======================================================

private double nextNegExp(double mean) {

	double u = CommonState.r.nextDouble();
	return ( - mean*Math.log(u) );
}


}
//--------------------------------------------------------------------------
//--------------------------------------------------------------------------

/**
* The type of a message. It contains a value of type double and the
* sender node of type {@link peersim.core.Node}.
*/
class ArrivedMessage {

	final int    typeOfMsg;
	final Node   sender;
	final int    stripeId;
	final int    msgParameter;
	public ArrivedMessage(int typeOfMsg, Node sender, int stripeId, int msgParameter )
	{
		this.typeOfMsg    = typeOfMsg;
		this.sender       = sender;
		this.stripeId     = stripeId;
		this.msgParameter = msgParameter;
	}
}
