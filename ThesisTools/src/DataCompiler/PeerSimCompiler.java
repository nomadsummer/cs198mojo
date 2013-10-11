package DataCompiler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import Grapher.Graph;
import Grapher.Tag;
import data.DataBit;
import data.DataSet;

public class PeerSimCompiler {

	public static final String TIME_FORMAT = "HH:mm:ss.SSS";
	public static final SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);

	public PeerSimCompiler() {

		// Note: Always add last '/'
		//String folderName = "results/TrueResults4/";
		//String folderName = "Results/1/"; //Normal
		String folderName = "Results/2/"; //True
		//String folderName = "Results/3/"; //False
		// String folderName = "results/Final-True_JD2/";
		// String folderName = "results/Final-True_JC1/";
		// String folderName = "results/Final-Fake-JC1/";
		// String folderName = "results/Final-Fake_EJ1/";
		// String folderName = "results/Final-OldFake_JC1/";

		// String folderName = "results/Old-Fake/";
		// int[] swarmSizes = { 10, 25, 50, 75, 100 };
		// int[] helpingSizes = { -100, -50, -20, 0, 20, 50, 100 };

		// int[] swarmSizes = { 10, 20, 30, 40, 50 };
		// int[] swarmSizes = { 10, 20, 30, 40, 50, 60, 70 };
		// int[] swarmSizes = { 10, 20, 30, 40, 50, 60, 70, 80, 90 };
		// int[] swarmSizes = { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
		// int[] swarmSizes = { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140 };
		int[] swarmSizes = { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
		// int[] helpingSizes = { -100, -90, -80, -70, -60, -50, -40, -30, -20, -10, 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
		//int[] helpingSizes = { 0, 20, 40, 60, 80, 100 };
		int[] helpingSizes = { -100, -80, -60, -40, -20, 0, 20, 40, 60, 80, 100 };
		// int[] helpingSizes = { -100, -75, -50, -25, 0, 25, 50, 75, 100 };

		// Output File Params
		String outputFile = "Master";

		// File Parsing Params
		// Object[] type = { Double.class, Integer.class, Double.class, Integer.class, Double.class };
		// int[] tagCount = { 4, 2, 6, 4, 4 };
		// String[] tags = { "[MOJO] Avg Playback Start: ", "Playbacks time-window: ", "Avg % of not played chunks: ", "Nodes with incomplete playbacks: ", "Perceived avg chunk delivery-time: " };
		// Object[] type = { Integer.class, Double.class, Double.class //};
		// int[] tagCount = { 2, 2, 3 };
		// String[] tags = { "Playbacks time-window: ", "[MOJO] Latency: ", "[MOJO] Packet Loss: " };
		Tag[] tags = { new Tag("Playbacks time-window: ", 2, Integer.class), //
				new Tag("[MOJO] Latency: ", 2, Double.class), //
				new Tag("[MOJO] Packet Loss: ", 3, Double.class), //
				new Tag("[MOJO] Startup Delay: ", 3, Double.class), //
				new Tag("[NONMOJO] Startup Delay: ", 3, Double.class), //
				new Tag("Avg messages sent per node: ", 5, Double.class), //
				new Tag("bandwidthUtilUp: ", 1, Double.class), //
				new Tag("bandwidthUtilDown: ", 1, Double.class), //
				new Tag("Avg distance between not played chunks: ", 6, Double.class), //
				new Tag("Rejected chunks for capacity limit [chunk-id/nodes]: ", 6, Integer.class), //
				new Tag("Rejected chunks for ttl expiration [chunk-id/nodes]: ", 6, Integer.class), //

				// OLD!
				// new Tag("Avg % of not played chunks: ", 6, Double.class), //
				new Tag("Nodes with incomplete playbacks: ", 4, Integer.class, true), //
		// new Tag("Perceived avg chunk delivery-time: ", 4, Double.class) //

		// Unwanted
		// new Tag("Avg # of unsent chunk for max-retries: ", 7, Double.class), //
		// new Tag("Avg # of unsent chunk-reqs for max-retries: ", 7, Double.class), //
		// new Tag("Avg % of chunks received by StarStream: ", 7, Double.class), //
		// new Tag("Avg % of chunks received by Pastry: ", 7, Double.class), //
		};

		Object[] obs = new Object[tags.length];

		DataSet[] dataSets = new DataSet[swarmSizes.length * helpingSizes.length];

		/*
		 * Data Loading
		 */
		for (int o = 0, c = 0; o < swarmSizes.length; o++) {
			for (int i = 0; i < helpingSizes.length; i++, c++) {
				FileReader input = null;
				try {
					// Init Dataset
					DataSet ds = new DataSet();

					// Open File
					String filename = folderName + swarmSizes[o] + "-" + helpingSizes[i] + ".log";
					input = new FileReader(filename);
					BufferedReader bufRead = new BufferedReader(input);

					// Read File
					String myLine = null;
					while ((myLine = bufRead.readLine()) != null) {
						// Check if line contains tag
						for (int t = 0; t < tags.length; t++) {
							if (myLine.startsWith(tags[t].tag)) {
								String[] parsed = myLine.split(" ");

								// Parse Value
								if (tags[t].type == Double.class) obs[t] = Double.parseDouble(parsed[tags[t].index]);
								else if (tags[t].type == Integer.class) obs[t] = Integer.parseInt(parsed[tags[t].index]);
							}
						}
					}

					// Create Data Object
					Object[] tObj = new Object[2 + tags.length];
					tObj[0] = (Integer) swarmSizes[o];
					tObj[1] = (Integer) helpingSizes[i];
					for (int t = 0; t < tags.length; t++)
						tObj[t + 2] = obs[t];

					// Add to DataSet
					ds.add(new DataBit(0, tObj));

					System.out.println("Size: " + ds.data.size());
					System.out.println(ds.data.get(0).toString());
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
		}

		/*
		 * Data Processing
		 */
		// DataSet dFinal = dataSets[0];
		// System.out.println("Num Obj: " + dFinal.data.get(0).data.length);
		// for (int i = 1; i < dataSets.length; i++) {
		// dFinal = DataSet.mergeSets(dFinal, dataSets[i]);
		// System.out.println("Num Obj: " + dFinal.data.get(0).data.length);
		// }
		// DataSet d2 = dataSets[0];
		// d2.debugLog();

		/*
		 * Data Output
		 */
		FileWriter output = null;
		try {
			output = new FileWriter(outputFile + ".txt");
			PrintWriter writer = new PrintWriter(output);

			for (int t = 0; t < tags.length; t++) {
				System.out.println(tags[t]);

				for (int i = 0; i < helpingSizes.length; i++)
					System.out.printf("\t" + helpingSizes[i]);
				System.out.println();

				// For Data
				int ctr = 0;
				Object[] data = new Object[helpingSizes.length * swarmSizes.length];

				for (int i = 0; i < swarmSizes.length; i++) {
					System.out.printf("" + swarmSizes[i]);
					for (int o = 0; o < helpingSizes.length; o++) {
						for (int x = 0; x < dataSets.length; x++) {
							// System.out.println("Looking for: " + swarmSizes[i] + " - " + helpingSizes[o] + " << " + dataSets[x].data.get(0).data[0] + " - " + dataSets[x].data.get(0).data[1]);
							// System.out.println(">> " + (dataSets[x].data.get(0).data[0].equals((Integer) swarmSizes[i])) + " >> " + (dataSets[x].data.get(0).data[1] == (Integer) helpingSizes[o]));
							if (dataSets[x].data.get(0).data[0].equals((Integer) swarmSizes[i]) && dataSets[x].data.get(0).data[1].equals((Integer) helpingSizes[o])) {
								// System.out.println("Found!");
								data[ctr++] = tags[t].process(swarmSizes[i], helpingSizes[i], dataSets[x].data.get(0).data[t + 2]);
								System.out.printf("\t" + data[ctr - 1]);
								break;
							}
						}
					}
					System.out.println();
				}

				// Graph It
				Graph g = new Graph(helpingSizes.length, swarmSizes.length, 64);
				g.setData(data);
				g.setLimits(0.0, 0.0);
				g.setAxes(helpingSizes, swarmSizes);
				// g.setColorScheme(new GrayScheme());
				g.buildImage(folderName + tags[t].tag.replace(":", "").replace(" ", "").replace("/", ""));
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

		for (double t = 0; t < 1; t += 0.001) {
			int r = (int) (255 * (t < 0.5 ? 0 : (t < 0.75 ? (t - 0.5) * 4.0 : 1)));
			int g = (int) (255 * (t < 0.75 ? (t < 0.25 ? t * 4.0 : 1) : (1 - t) * 4.0));
			int b = (int) (255 * (t < 0.5 ? (t < 0.25 ? 1 : 1 - (t - 0.25) * 4.0) : 0));
			System.out.println("RGB: " + r + ", " + g + ", " + b);
		}

		System.out.println("--- Program Started ---");

		new PeerSimCompiler();

		System.out.println("--- Program Ended ---");
	}

}
