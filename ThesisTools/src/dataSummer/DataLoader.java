package dataSummer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import data.DataBit;
import data.DataSet;

public class DataLoader {

	public static final String TIME_FORMAT = "HH:mm:ss.SSS";
	public static final SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);

	public DataLoader() {

		// Input File Params
		String fileBase = "chunksplayed[time,id,#ofplayedchunks]";

		// Output File Params
		String outputFile = fileBase + "__parsed";

		// File Parsing Params
		String tag = "[MOJO]";

		/*
		 * Data Loading
		 */
		DataSet ds = new DataSet();
		FileReader input = null;
		try {
			// Open File
			String filename = fileBase + ".txt";
			input = new FileReader(filename);
			BufferedReader bufRead = new BufferedReader(input);
			System.out.println("Reading File: " + filename);

			// Read File
			String myLine = null;
			while ((myLine = bufRead.readLine()) != null) {
				// Check if line contains tag
				if (myLine.startsWith(tag)) {
					String[] parsed = myLine.split("\t");

					// Parse Take Timestamp [1]
					long timestamp = 0;
					String[] timeString = parsed[1].split(":");
					timestamp = Integer.parseInt(parsed[1]);

					// Parse PID [2]
					Integer pid = Integer.parseInt(parsed[2]);

					// Parse Played Chunks [4]
					Integer pChunks = Integer.parseInt(parsed[4]);

					// Add to DataSet
					DataBit d = new DataBit(timestamp, new Object[] { pid, pChunks });
					ds.add(d);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/*
		 * Data Processing
		 */
		long time = 0;
		List<DataSet> lists = new ArrayList<DataSet>();
		for (int i = 0; i < ds.data.size(); i++) {
			if (ds.data.get(i).time != time) {
				time = ds.data.get(i).time;
				DataSet td = ds.filterBy(time, -1);
				lists.add(td);
			}
		}
		double[] avgs = new double[lists.size()];
		for (int i = 0; i < lists.size(); i++) {
			double sum = 0;
			DataSet tds = lists.get(i);
			for (int o = 0; o < tds.data.size(); o++) {
				sum += (Integer) tds.data.get(o).data[1];
			}
			avgs[i] = sum / tds.data.size();
		}

		/*
		 * Data Output
		 */
		FileWriter output = null;
		try {
			output = new FileWriter(outputFile + ".txt");
			PrintWriter writer = new PrintWriter(output);

			for (int i = 0; i < lists.size(); i++) {
				writer.println(lists.get(i).data.get(0).time + "\t" + avgs[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		System.out.println("--- Program Started ---");

		new DataLoader();

		System.out.println("--- Program Ended ---");
	}

}
