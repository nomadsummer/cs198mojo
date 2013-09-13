package peersim.graph;

import java.awt.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class BencMark {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		int num = 1000000;
		int prime1 = 12473;
		int prime2 = 16729;
		HashMap<Integer, Float> intFloatHash = new HashMap<Integer, Float>();
		HashMap<String, Float> strFloatHash = new HashMap<String, Float>();
		
		double putTimeInteger = 0;
		double putTimeString = 0;
		Random r = new Random();   
		int[] II = new int[num];
		int[] JJ = new int[num];
		for (int k= 0; k < num; k++){
			II[k] = r.nextInt();
			JJ[k] = r.nextInt();
			
		}
		
		double t1,t2;
		double total=0;
		for (int k= 0; k < num; k++) {
			int i = II[k];
			int j = JJ[k];
			t1 = System.currentTimeMillis();
			strFloatHash.put(i+"-"+j, 56f);
			t2 = System.currentTimeMillis();
			
			total+= (t2-t1);
			
		}
		System.out.println("String hash put-time="+ total);
		
		total=0;
		for (int k= 0; k < num; k++) {
			int i = II[k];
			int j = JJ[k];
			t1 = System.currentTimeMillis();
			
			intFloatHash.put(i*prime1+j*prime2, 56f);
			t2 = System.currentTimeMillis();
			total+= (t2-t1);
				
			
		}
		System.out.println("Integer hash put-time="+ total);
		
		total =0;
		for (int k= 0; k < num; k++) {
			int i = II[k];
			int j = JJ[k];
			t1 = System.currentTimeMillis();
			strFloatHash.get(i+"-"+j);
			t2 = System.currentTimeMillis();
			total += (t2-t1);
			
		}
		System.out.println("String hash get-time="+ total );
		
		total =0;
		for (int k= 0; k < num; k++) {
			int i = II[k];
			int j = JJ[k];
			t1 = System.currentTimeMillis();
			intFloatHash.get(i*prime1+j*prime2);//, 56f);
			t2 = System.currentTimeMillis();
			total += (t2-t1);
			
		}
		System.out.println("Integer hash get-time="+ total );
		

		
		total =0;
		for (int k= 0; k < num; k++) {
			int i = II[k];
			int j = JJ[k];
			t1 = System.currentTimeMillis();
			strFloatHash.containsKey(i+"-"+j);
			t2 = System.currentTimeMillis();
			total += (t2-t1);
			
		}
		System.out.println("String hash Contains="+ total );
		
		total =0;
		for (int k= 0; k < num; k++) {
			int i = II[k];
			int j = JJ[k];
			t1 = System.currentTimeMillis();
			intFloatHash.containsKey(i*prime1+j*prime2);//, 56f);
			t2 = System.currentTimeMillis();
			total += (t2-t1);
			
		}
		System.out.println("Integer hash Contains="+ total );
		
		
	}

}
