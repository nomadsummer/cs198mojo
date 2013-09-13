package peersim.simildis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class MsgBuffer {

	private List<MSG> buffer;
	private int  buffMaxSize;
	private int maxAge ; 
	
	public int getBuffMaxSize() {
		return buffMaxSize;
	}

	public  MsgBuffer(int maxBufferSize, byte maxAge){
		this.buffMaxSize = maxBufferSize;
		this.maxAge = maxAge;
		this.buffer = new ArrayList<MSG>();
	}
	
	
	public void addNewRecord(int peer_id_from , int peer_id_to, byte initAge){
		if (initAge > this.maxAge) return;
		MSG m = new MSG(peer_id_from, peer_id_to, initAge);
		if (!this.buffer.contains(m)){
			if(this.buffer.size() == this.buffMaxSize){
				if (this.removeOldRecords(1)==1){
					this.buffer.add(m);
					this.sort();
					}
				}
				else{
					this.buffer.add(m);
					this.sort();
			}
		}
	}
	
	
	public boolean increaseAge(MSG msg){
		if (!this.buffer.contains(msg)){
			return false;
		}
		
		int index = this.buffer.lastIndexOf(msg);
		this.buffer.get(index).age +=1;
		return true;
	}
	
	public boolean updateAge(int peer_id_from , int peer_id_to, byte newAge){
		
		if (newAge > this.maxAge) return false;
		MSG m = new MSG(peer_id_from,peer_id_to, newAge);
		
		if (newAge == this.maxAge) {
			this.buffer.remove(m);
		}
		
		if (this.buffer.contains(m)){
			int i = this.buffer.lastIndexOf(m);
			if (i!=-1){
				this.buffer.get(i).changeAge(newAge);
				this.sort();
				return true;
			}
		
		}
		else{
			this.addNewRecord(peer_id_from, peer_id_to, newAge);
			return true;
		}
		return false;
	}
	
	
	public void sort(){
		Comparator<MSG> r = Collections.reverseOrder();
		Collections.sort(this.buffer,r);
	}
	
	public int removeOldRecords(int numOfRecords){
		if (this.buffer.isEmpty()) return 0;
		if (numOfRecords > this.buffer.size()){
			int bsize = this.buffer.size();
			this.buffer.clear();
			return bsize;
		}
		
		List<MSG> temp_list = new ArrayList<MSG>();
		int count = 0; 
		
		boolean stop_more = false;

		int buff_size = this.buffer.size();
		int j=0;
		
				j =0;
				while (j < buff_size-1 && !stop_more) {
					MSG msg = this.buffer.get(j);
					temp_list.add(msg);
					count +=1;
					if (this.buffer.get(j+1).age!=msg.age)
						if (count > numOfRecords)
							stop_more = true;	
					j+=1;
				}
				
		
		count = 0;
		Comparator<MSG> r = Collections.reverseOrder();
		Collections.sort(temp_list,r);
		List<MSG> mm = new ArrayList<MSG>();
		int temp_list_size = temp_list.size();
		int k =0;
		while ( k < temp_list_size  && count < numOfRecords) {
			mm.clear();
			int k2 = k;
			boolean bb = false;
			while(k2 < temp_list_size && count < numOfRecords && !bb){
				
				mm.add(temp_list.get(k2));
				if (k2 < temp_list_size-1){
					if (temp_list.get(k2).age != temp_list.get(k2+1).age )
					{
						Collections.shuffle(mm);
						for (Iterator iterator = mm.iterator(); iterator.hasNext();) {
							MSG msg = (MSG) iterator.next();
							this.buffer.remove(msg);
							count+=1;
						}
						mm.clear();
						bb = true;
					}
			}
				else{
					Collections.shuffle(mm);
					for (Iterator iterator = mm.iterator(); iterator.hasNext();) {
						if (count >= numOfRecords) break;
						MSG msg = (MSG) iterator.next();
						this.buffer.remove(msg);
						count+=1;
						
					}
					mm.clear();
					bb = true;
					
				}
				k2 +=1;
			}
			k = k2;
		}
		for (Iterator iterator = temp_list.iterator(); iterator.hasNext();) {
			MSG msg = (MSG) iterator.next();
			if (count > numOfRecords) break;
			this.buffer.remove(msg);
			count +=1;
		}
		return numOfRecords;
		
	}
	
	
	public void removeSomeRecords(int numberOfRecords){
		if ( numberOfRecords > this.buffer.size())
		{
			this.buffer.clear();
			return;
		}
		Collections.shuffle(this.buffer);
		for (int i = 0; i < numberOfRecords; i++) {
			this.buffer.remove(i);
			
		}
		this.sort();
		return;
	}
	
	
	public List<MSG> getFreshRecords(int numOfRecords){
		
		List<MSG> res = new ArrayList<MSG>();
		
		if (this.buffer.isEmpty()) return null;
		if (numOfRecords > this.buffer.size()){
			return this.buffer;
		}
		
		List<MSG> temp_list = new ArrayList<MSG>();
		int count = 0; 
		
		boolean stop_more = false;
		
		int buff_size = this.buffer.size();
		int j=buff_size-1;
		while (j > 0 && !stop_more) {
			MSG msg = this.buffer.get(j);
			temp_list.add(msg);
			count +=1;
			if (this.buffer.get(j-1).age!=msg.age)
				if (count > numOfRecords)
					stop_more = true;	
			j-=1;
		}
				
		
		//log("Records to return:"+temp_list.toString());
		count = 0;
		
		
		Collections.sort(temp_list);
		List<MSG> mm = new ArrayList<MSG>();
		int temp_list_size = temp_list.size();
		int k =0;
		while ( k < temp_list_size  && count < numOfRecords) {
			mm.clear();
			int k2 = k;
			boolean bb = false;
			while(k2 < temp_list_size && count < numOfRecords && !bb){
				
				mm.add(temp_list.get(k2));
				if (k2 < temp_list_size-1){
					if (temp_list.get(k2).age != temp_list.get(k2+1).age )
					{
						Collections.shuffle(mm);
						for (Iterator iterator = mm.iterator(); iterator.hasNext();) {
							MSG msg = (MSG) iterator.next();
							res.add(msg);
							count+=1;
						}
						mm.clear();
						bb = true;
					}
			}
				else{
					Collections.shuffle(mm);
					for (Iterator iterator = mm.iterator(); iterator.hasNext();) {
						if (count >= numOfRecords) break;
						MSG msg = (MSG) iterator.next();
						res.add(msg);
						count+=1;
						
					}
					mm.clear();
					bb = true;
					
				}
				k2 +=1;
			}
			k = k2;
		}
		
		return res;
	}
	
	
	public String toString(){
		String s = "Buffer:{";
		for (Iterator iterator = this.buffer.iterator(); iterator.hasNext();) {
			MSG msg  = (MSG)iterator.next();
			s+=msg.toString()+" ";			
		}
		return s+"}";
	}
	
	public static void log(String s){
		System.out.println(s);
	}
	
	public static void main(String args[]){
		
		MsgBuffer b = new MsgBuffer(15, (byte)1);
		b.addNewRecord(1, 2, (byte)0);
		b.addNewRecord(2, 3, (byte)0);
		b.addNewRecord(4, 6, (byte)0);
		
		b.addNewRecord(7, 8, (byte)1);
		b.addNewRecord(2, 4, (byte)1);
		b.addNewRecord(24, 2, (byte)1);
		
		b.addNewRecord(17, 18, (byte)2);
		b.addNewRecord(12, 14, (byte)2);
		b.addNewRecord(117, 118, (byte)2);
		b.addNewRecord(112, 114, (byte)2);
		b.addNewRecord(124, 12, (byte)6);
		
		log(b.toString());
		log("After removal");
		log("Fresh records:"+b.getFreshRecords(2));
		
		
		log(b.toString());
		
	}
	
	
	class MSG implements Comparable<MSG>{
		
		public MSG(int peer_id_from , int peer_id_to, byte initAge){
			this.peer_id_from = peer_id_from;
			this.peer_id_to = peer_id_to;
			this.age = initAge;
			
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + age;
			result = prime * result + peer_id_from;
			result = prime * result + peer_id_to;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MSG other = (MSG) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			//if (age != other.age)
			//	return false;
			if (peer_id_from != other.peer_id_from)
				return false;
			if (peer_id_to != other.peer_id_to)
				return false;
			return true;
		}
		int peer_id_from;
		int peer_id_to;
		byte age;
		private MsgBuffer getOuterType() {
			return MsgBuffer.this;
		}
		
		public int compareTo(MSG o2){
			
			if (this.age < o2.age)
				return -1;
			else if  (this.age == o2.age)
				return 0;
			else 
				return +1;
		}
		
		public void changeAge(byte newAge){
			this.age = newAge;
		}
		
		public String toString(){
			return "("+peer_id_from+"-->"+peer_id_to+":"+age+")";
		}
		
		
	}
	
	
	
}
