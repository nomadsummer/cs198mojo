package peersim.ps.types;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;
import java.util.Vector;

import peersim.core.CommonState;
import peersim.ps.utility.Pareto;


public class Buckets {
	private final int numberOfBuckets;
	private int min = 1;
	private int lenght;
	
//------------------------------------------------------------------------
	public Buckets(int numberOfTopics, int numberOfBuckets) {
//		this.numberOfTopics = numberOfTopics;
		this.numberOfBuckets = numberOfBuckets;
		this.lenght = (numberOfBuckets > 0 ? numberOfTopics / numberOfBuckets: 0);
	}
	
//------------------------------------------------------------------------
	public Vector<Integer> generateSample(int size, int numberOfSelectedBuckets, double alpha) {
		Double d;
		Integer i;
		double bucketBegin;
		double bucketEnd;
		double bucketSample;
		Pareto insideBucket;
		int numberOfSamplesPerBucket;
//		Pareto pareto = new Pareto(alpha, rnd.nextInt());
		Vector<Integer> samples = new Vector<Integer>();
		Vector<Integer> selectedBuckets = new Vector<Integer>();
		Vector<Integer> distinctSamples = new Vector<Integer>();

		if (size < numberOfSelectedBuckets)
			numberOfSamplesPerBucket = 1;
		else
			numberOfSamplesPerBucket = size / numberOfSelectedBuckets;

		while (distinctSamples.size() < size) {
//			j = 0;
//			do {
//				p = pareto.nextBoundedNumber(min, max);
//				i = new Integer(getBucketNumber(p));
//				j++;
//
//			} while (selectedBuckets.contains(i) && j < numberOfBuckets);

			i = new Random().nextInt(numberOfBuckets);
			selectedBuckets.add(i);
			
			bucketBegin = (min + i * lenght);
			bucketEnd = bucketBegin + lenght;
			
			insideBucket = new Pareto(alpha, CommonState.r.nextInt());
			
			for (int k = 0; k < numberOfSamplesPerBucket; k++) {
				bucketSample = insideBucket.nextBoundedNumber(bucketBegin, bucketEnd);
				d = new Double(bucketSample);
				i = new Integer((int) (d.doubleValue()));
			
				if (!distinctSamples.contains(i)) {
					distinctSamples.add(i);
				} else
					k--;
				
				if (!samples.contains(i))
					samples.add(i);
			}			
		}
			
		return samples;
	}	
	
//------------------------------------------------------------------------
	public double getBucketBegin(double u) {
		int i;
		
		for (i = 0; i < numberOfBuckets; i++) {
			if (u > min + (i * lenght) && u < min + ((i + 1) * lenght))
				break;
		}
		
		return (min + i * lenght);		
	}

	
//------------------------------------------------------------------------
	public int getBucketNumber(double u) {
		int i;
		
		for (i = 0; i < numberOfBuckets; i++) {
			if (u > min + (i * lenght) && u < min + ((i + 1) * lenght))
				break;
		}
		
		return i;		
	}
	
//------------------------------------------------------------------------
	public static void main(String args[]) {
		Buckets b = new Buckets(1000, 1);
		int[] histogram = new int[1001];		
		Vector<Integer> samples;
		Vector<Integer> distinctSamples = new Vector<Integer>();
		
		for (int i = 0; i < 1001; i++)
			histogram[i] = 0;
		
		Integer i;
		
		for (int k =0; k < 1000; k++) {
			samples = b.generateSample(100, 1, 0.1);
			distinctSamples.clear();
			
			for (Integer d : samples) {
				i = new Integer((int) (d.doubleValue()));

				if (!distinctSamples.contains(i)) {
					distinctSamples.add(i);
					System.out.print(i.intValue() + " ");
					histogram[i.intValue()]++;
				}
			}

			System.out.println();			
		}
		
		System.out.println("--------------------------");
		String str = "";
		
		for (int j = 0; j < 1001; j++) {
//			System.out.println(j + " " + histogram[j]);
			str += j + " " + histogram[j] + "\n";
		}
	    
		try {
	    	    // Create/Append file for ring
	    	    FileWriter fstream = new FileWriter("/home/fatemeh/mynotes/data.dot",false);
	    	    BufferedWriter out = new BufferedWriter(fstream);
	    	    out.write(str);
	    	    //Close the output stream
	    	    out.close();
	    } catch (Exception e) {
			// TODO: handle exception
		}
	}

}