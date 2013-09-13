package peersim.simildis.utils;



public class SimpleEdge implements Comparable<SimpleEdge>{
	public  final int i;
	public int getI() {
		return i;
	}

	public int getJ() {
		return j;
	}

	public final int j;
	public SimpleEdge(int i , int j){
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