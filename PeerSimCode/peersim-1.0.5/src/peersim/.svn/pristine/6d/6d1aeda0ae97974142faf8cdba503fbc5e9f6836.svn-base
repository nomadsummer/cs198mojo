package peersim.simildis;

import java.io.IOException;  
import java.io.RandomAccessFile;  
   
public class RAF {  
public static void main(String[] args) throws IOException {  
// TODO Auto-generated method stub  
RandomAccessFile raf = new RandomAccessFile("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/pubsub/src/peersim/simildis/traces/sample_trace.txt", "r");  
//String s = "XXXXXXXXXXXXXXXX";  
//raf.writeBytes(s);  
long len = raf.length();  
System.out.println(len);  

//String s2 = "YYYYYYYYYYYYYYYYYYY";  
//raf.writeBytes(s2);  
raf.seek(3*24);  
byte[] b = new byte[24];  
raf.read(b);  
String s3 = new String(b);  
System.out.print(s3);
raf.close();  
}  
}  