package peersim.simildis;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import peersim.simildis.utils.WeightedEdge;
import peersim.util.*;

public class ProcessRuntimeResult {

	/**
	 * @param args
	 */
	
	public void logCumulativeRuntime(String rawResultFileName, String staticResultFileName, String outputFile){
		Scanner scanner;
		int lineKey = 0;
		
		HashMap<Integer, IncrementalStats> runtimeRes = new HashMap<Integer, IncrementalStats>();

		try {
			scanner = new Scanner(new FileReader(rawResultFileName));
			double accRuntime = 0.0;
			int  prev_exp = 1;
			      while ( scanner.hasNextLine() ){
			    	  
			    	  String s = scanner.nextLine();
			    	  if (s.charAt(0)=='#')
			    		 continue;
			    	  String[] lineStr = s.split("\\|");
			    	  int exp = Integer.parseInt(lineStr[0].replaceAll(" ", ""));
			    	  int numOfRecs = Integer.parseInt(lineStr[1].replaceAll(" ", ""));
			    	  double runtime = Double.parseDouble(lineStr[2].replaceAll(" ", ""));
			    	  
			    	  if (exp!=prev_exp){
			    		  accRuntime = 0.0;
			    		  prev_exp = exp;
			    	  }
			    	  
			    	  accRuntime +=runtime;
			    	  if (!runtimeRes.keySet().contains(numOfRecs)){
			    		  runtimeRes.put(numOfRecs, new IncrementalStats());
			    	  }
			    	  runtimeRes.get(numOfRecs).add(accRuntime);
			    	  
			    	  lineKey ++;
			    	  
			    	  }
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			scanner = new Scanner(new FileReader(staticResultFileName));
			PrintStream out = new PrintStream(new FileOutputStream(outputFile));
			out.println("#num_records|num_nodes|num_edges|density|avg_runtime|se_runtime|mode");
			
			while ( scanner.hasNextLine() ){
			    	  
			   	  String s = scanner.nextLine();
			   	  if (s.charAt(0)=='#')
			   		 continue;
			   	  String[] lineStr = s.split("\\|");
			    	  
			   	  int num_records = Integer.parseInt(lineStr[0].replaceAll(" ", ""));
			   	  if (runtimeRes.keySet().contains(num_records)){
			   		  
			   		  out.println(s+"|Static");//+runtimeRes.get(num_records).getAverage()+"|"+runtimeRes.get(num_records).getStE());
			   	  
			   	  }
			   	    
			}
			Scanner scanner2 = new Scanner(new FileReader(staticResultFileName));
			while ( scanner2.hasNextLine() ){
		    	  
			   	  String s = scanner2.nextLine();
			   	  if (s.charAt(0)=='#')
			   		 continue;
			   	  String[] lineStr = s.split("\\|");
			    	  
			   	  int num_records = Integer.parseInt(lineStr[0].replaceAll(" ", ""));
			   	  int num_nodes = Integer.parseInt(lineStr[1].replaceAll(" ", ""));
			   	  int  num_edges = Integer.parseInt(lineStr[2].replaceAll(" ", ""));
			   	  double  density = Double.parseDouble(lineStr[3].replaceAll(" ", ""));
			   	  
			   	  if (runtimeRes.keySet().contains(num_records)){
			   		  
			   		  out.println(num_records+"|"+num_nodes+"|"+num_edges+"|"+density+"|"+runtimeRes.get(num_records).getAverage()+"|"+runtimeRes.get(num_records).getStE()+"|Dynamic");
			   	  
			   	  }
			   	    
			}
			
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void logRuntime(String rawResultFileName, String staticResultFileName, String outputFile){
		Scanner scanner;
		int lineKey = 0;
		
		HashMap<Integer, IncrementalStats> runtimeRes = new HashMap<Integer, IncrementalStats>();

		try {
			scanner = new Scanner(new FileReader(rawResultFileName));
			
			int  prev_exp = 1;
			      while ( scanner.hasNextLine() ){
			    	  
			    	  String s = scanner.nextLine();
			    	  if (s.charAt(0)=='#')
			    		 continue;
			    	  String[] lineStr = s.split("\\|");
			    	  int exp = Integer.parseInt(lineStr[0].replaceAll(" ", ""));
			    	  int numOfRecs = Integer.parseInt(lineStr[1].replaceAll(" ", ""));
			    	  double runtime = Double.parseDouble(lineStr[2].replaceAll(" ", ""));
			    	  
			    	  if (!runtimeRes.keySet().contains(numOfRecs)){
			    		  runtimeRes.put(numOfRecs, new IncrementalStats());
			    	  }
			    	  runtimeRes.get(numOfRecs).add(runtime);
			    	  
			    	  lineKey ++;
			    	  
			    	  }
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			scanner = new Scanner(new FileReader(staticResultFileName));
			PrintStream out = new PrintStream(new FileOutputStream(outputFile));
			out.println("#num_records|num_nodes|num_edges|density|avg_runtime|se_runtime|mode");
			
			while ( scanner.hasNextLine() ){
			    	  
			   	  String s = scanner.nextLine();
			   	  if (s.charAt(0)=='#')
			   		 continue;
			   	  String[] lineStr = s.split("\\|");
			    	  
			   	  int num_records = Integer.parseInt(lineStr[0].replaceAll(" ", ""));
			   	  if (runtimeRes.keySet().contains(num_records)){
			   		  
			   		  out.println(s+"|Static");//+runtimeRes.get(num_records).getAverage()+"|"+runtimeRes.get(num_records).getStE());
			   	  
			   	  }
			   	    
			}
			Scanner scanner2 = new Scanner(new FileReader(staticResultFileName));
			while ( scanner2.hasNextLine() ){
		    	  
			   	  String s = scanner2.nextLine();
			   	  if (s.charAt(0)=='#')
			   		 continue;
			   	  String[] lineStr = s.split("\\|");
			    	  
			   	  int num_records = Integer.parseInt(lineStr[0].replaceAll(" ", ""));
			   	  int num_nodes = Integer.parseInt(lineStr[1].replaceAll(" ", ""));
			   	  int  num_edges = Integer.parseInt(lineStr[2].replaceAll(" ", ""));
			   	  double  density = Double.parseDouble(lineStr[3].replaceAll(" ", ""));
			   	  
			   	  if (runtimeRes.keySet().contains(num_records)){
			   		  
			   		  out.println(num_records+"|"+num_nodes+"|"+num_edges+"|"+density+"|"+runtimeRes.get(num_records).getAverage()+"|"+runtimeRes.get(num_records).getStE()+"|Dynamic");
			   	  
			   	  }
			   	    
			}
			
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}	
	
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String rawResultFileName = args[0];  // result file from running the simildis simulator.
		String staticResultFileName = args[1]; // result file from running the static version of the update.
		String outputFile = args[2];           // the combined output file
		logRuntime( rawResultFileName,  staticResultFileName,  outputFile);
		
	}

}
