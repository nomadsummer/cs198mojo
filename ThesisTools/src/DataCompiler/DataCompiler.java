package DataCompiler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import data.DataBit;
import data.DataSet;

public class DataCompiler {

	public static final String TIME_FORMAT = "HH:mm:ss.SSS";
	public static final SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);

	public DataCompiler() {

		// Input File Params
		String fileBase = "09-12Peer";
		int firstIndex = 1;
		int lastIndex = 10;

		// Output File Params
		String outputFile = "Master";

		// File Parsing Params
		String tag = "[MJ-ClientStats]";

		DataSet[] dataSets = new DataSet[lastIndex - firstIndex + 1];

		/*
		 * Data Loading
		 */
		for (int i = firstIndex, c = 0; i <= lastIndex; i++, c++) {
			FileReader input = null;
			try {
				// Init Dataset
				DataSet ds = new DataSet();

				// Open File
				String filename = fileBase + i + ".txt";
				input = new FileReader(filename);
				BufferedReader bufRead = new BufferedReader(input);
				System.out.println("Reading File: " + filename);

				// Read File
				String myLine = null;
				while ((myLine = bufRead.readLine()) != null) {
					// Check if line contains tag
					// System.out.println("myline: " + myLine);
					if (myLine.startsWith(tag)) {
						String[] parsed = myLine.split("\t");

						// Parse Take Timestamp
						long timestamp = 0;
						String[] timeString = parsed[1].split(":");
						timestamp = Integer.parseInt(timeString[0]) * 60 * 60 * 1000 + Integer.parseInt(timeString[1]) * 60 * 1000 + (int) (Double.parseDouble(timeString[2]) * 1000);

						// Parse DL [7]
						Double dl = null;
						try {
							dl = Double.parseDouble(parsed[7]);
						} catch (Exception e) {
							System.out.println(">>" + myLine);
							e.printStackTrace();
						}

						// Parse UL [8]
						// Double ul = Double.parseDouble(parsed[9]);

						// Add to DataSet
						// DataBit d = new DataBit(timestamp, new Object[] { dl, ul });
						DataBit d = new DataBit(timestamp, new Object[] { dl });
						ds.add(d);

						// System.out.println(d);
					}
				}

				dataSets[c] = ds;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		/*
		 * Data Processing
		 */
		DataSet dFinal = dataSets[0];
		System.out.println("Num Obj: " + dFinal.data.get(0).data.length);
		for (int i = 1; i < dataSets.length; i++) {
			dFinal = DataSet.mergeSets(dFinal, dataSets[i]);
			System.out.println("Num Obj: " + dFinal.data.get(0).data.length);
		}
		// DataSet d2 = dataSets[0];
		// d2.debugLog();

		/*
		 * Data Output
		 */
		FileWriter output = null;
		try {
			output = new FileWriter(outputFile + ".txt");
			PrintWriter writer = new PrintWriter(output);

			for (int i = 0; i < dFinal.data.size(); i++) {
				// System.out.println(">>" + dFinal.data.get(i).toString());
				writer.println(dFinal.data.get(i).toString());
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

		new DataCompiler();

		System.out.println("--- Program Ended ---");
	}

}
