package peersim.ps.utility;

import java.io.*;
import java.util.Scanner;

public class ReadWithScanner {

  public static void main(String[] args) throws FileNotFoundException {
    /*ReadWithScanner parser = new ReadWithScanner(args[0]);
    int[][] r = parser.processLineByLine(4,2);
    for (int i = 0; i < r.length; i++) {
		for (int j = 0; j < 3; j++) {
			log(r[i][j]+" ");
		}
		log("\n");
	}*/
  }
  

  public ReadWithScanner(String aFileName){
    fFile = new File(aFileName);  
  }
  
  public final int[][] processLineByLine(int numberOfLines) throws FileNotFoundException {
	int[][] result = new int[numberOfLines][3] ;
	  
    Scanner scanner = new Scanner(new FileReader(fFile));
    int counter = 0;
    int lineNumber = 0;
    int startLine = -1;
    try {
      while ( scanner.hasNextLine() && counter < numberOfLines ){
    	  if (lineNumber > startLine){
    		  int[] record = processLine( scanner.nextLine() );
    		  if (record != null ){
    			  result[counter][0]=record[0];
    			  result[counter][1]=record[1];
    			  result[counter][2]=record[2];
    			  counter +=1;
    			  
    		  }
    	  }
    	  else{
    		  scanner.nextLine();
    	  }
    	  lineNumber +=1;
      }
    }
    finally {
      scanner.close();
    }
    return result;
  }
  

  protected int[] processLine(String aLine){
    //use a second Scanner to parse the content of each line
	int[] r = new int[3];
    if (aLine.charAt(0)=='#')
    	return null;
    String[] lineStr = aLine.split("\\|");
    r[0] = Integer.parseInt(lineStr[0].replaceAll(" ", ""));
    r[1] = Integer.parseInt(lineStr[1].replaceAll(" ", ""));
    r[2] = Integer.parseInt(lineStr[2].replaceAll(" ", ""));
    
    
    return r;
  }
  // PRIVATE 
  private final File fFile;
  
  private static void log(Object aObject){
    System.out.print(String.valueOf(aObject));
  }
  
  private String quote(String aText){
    String QUOTE = "'";
    return QUOTE + aText + QUOTE;
  }
  
 
} 
