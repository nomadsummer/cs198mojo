package Grapher;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import Grapher.colorscheme.ColorScheme;
import Grapher.colorscheme.HeatScheme;

public class Graph {

	BufferedImage img;
	int w, h;
	int aw, ah;
	int mult;
	int imgSize;

	Object[] data;
	double[] limits = new double[] { 0.0, 1.0 };

	int[] xAxis;
	int[] yAxis;

	ColorScheme colorScheme = new HeatScheme();

	public Graph(int w, int h) {
		this(w, h, 1);
	}

	public Graph(int w, int h, int mult) {
		this.w = w;
		this.h = h;
		this.mult = mult;
		aw = w * mult - (mult - 1);
		ah = h * mult - (mult - 1);
		imgSize = aw * ah;

		img = new BufferedImage(aw, ah, BufferedImage.TYPE_INT_ARGB);
	}

	public void setLimits(double min, double max) {
		limits = new double[] { min, max };
	}

	public void setData(Object[] d) {
		this.data = d;
	}

	public void setAxes(int[] x, int[] y) {
		this.xAxis = x;
		this.yAxis = y;
	}

	public void setColorScheme(ColorScheme c) {
		this.colorScheme = c;
	}

	public void buildImage(String filename) {
		double t;
		System.out.println("Data Length: " + data.length);
		System.out.println("Image Size: " + imgSize);
		System.out.println("Image Dim: " + aw + ", " + ah);

		// 0) Interpolate Data
		double[] newData = new double[aw * ah];
		// 0.1) Set All to -Infinity.
		for (int i = 0; i < newData.length; i++)
			newData[i] = 0;
		// 0.2) Set all known data points
		for (int i = 0, c = 0; i < newData.length; i++) {
			int x = i % aw;
			int y = i / aw;
			if (x % mult == 0 && y % mult == 0) {
				newData[i] = ((Number) data[c++]).doubleValue();
			}
		}
		// 0.3) Interpolate
		for (int sax = mult; sax > 1; sax /= 2) {
			// System.out.println(">> NEW SAX:" + sax);

			for (int i = 0; i < newData.length; i++) {
				int x = i % aw;
				int y = i / aw;

				if (x % sax == 0 && y % sax == 0) continue; // Skip Data Points
				if (x % sax == 0 || y % sax == 0) {
					// Between 2 Data Points
					if (y % sax == 0 && x % (sax / 2) == 0) { // Between 2 Xs
						// System.out.println("> " + x + ", " + y);
						newData[y * aw + x] = (newData[y * aw + x + (sax / 2)] + newData[y * aw + x - (sax / 2)]) / 2;
					} else if (x % sax == 0 && y % (sax / 2) == 0) { // Between 2 Ys
						// System.out.println("< " + x + ", " + y);
						newData[y * aw + x] = (newData[(y + (sax / 2)) * aw + x] + newData[(y - (sax / 2)) * aw + x]) / 2;
					}
				} else {
				}
			}
			for (int i = 0; i < newData.length; i++) {
				int x = i % aw;
				int y = i / aw;

				if (x % sax == sax / 2 && y % sax == sax / 2) {
					newData[y * aw + x] = (newData[(y + (sax / 2)) * aw + x] + newData[(y - (sax / 2)) * aw + x] + newData[y * aw + x + (sax / 2)] + newData[y * aw + x - (sax / 2)]) / 4;
				}
			}
		}

		// 1) Make Data Within Limits
		// 1.1) Check Validity of Limits
		if (limits[0] == limits[1]) {
			// Auto.
			limits[0] = newData[0];
			limits[1] = newData[1];
			for (int i = 0; i < newData.length; i++) {
				if (newData[i] < limits[0]) limits[0] = newData[i];
				if (newData[i] > limits[1]) limits[1] = newData[i];
			}
		}
		if (limits[1] - limits[0] < 1) {
		} else {
			limits[0] = Math.floor(limits[0]);
			limits[1] = Math.ceil(limits[1]);
		}
		System.out.println("Limits: [" + limits[0] + ", " + limits[1] + "]");

		for (int i = 0; i < imgSize; i++) {
			t = (newData[i] - limits[0]) / (limits[1] - limits[0]);
			// int r = (int) (255 * (t < 0.5 ? 0 : (t < 0.75 ? (t - 0.5) * 4.0 : 1)));
			// int g = (int) (255 * (t < 0.75 ? (t < 0.25 ? t * 4.0 : 1) : (1 - t) * 4.0));
			// int b = (int) (255 * (t < 0.5 ? (t < 0.25 ? 1 : 1 - (t - 0.25) * 4.0) : 0));
			// System.out.println("RGB: " + r + ", " + g + ", " + b);

			img.setRGB(i % aw, ah - 1 - i / aw, colorScheme.getColor(t).getRGB());
		}

		// 2) Add Axes
		int axesSize = 64;
		int tRP = 16; // Top Right Padding
		int barLines = 16;
		int legendSize = 64;
		int legendSpace = 128;
		BufferedImage tImg = new BufferedImage(aw + axesSize + tRP, ah + axesSize + tRP, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) tImg.getGraphics();
		GraphicsFontUtil gfu = new GraphicsFontUtil(g);
		// 2.1) Add Graph to Resized Image
		g.setColor(Color.white);
		g.fillRect(0, 0, tImg.getWidth(), tImg.getHeight());
		g.drawImage(img, axesSize, tRP, null);
		// 2.2) Add Black Lines
		g.setColor(Color.black);
		g.drawLine(axesSize, tRP, axesSize, ah + tRP); // Y-Axis
		g.drawLine(axesSize, ah + tRP, axesSize + aw, ah + tRP); // X-Axis
		// 2.3) Add Interval Lines + Labels
		double xR = xAxis[xAxis.length - 1] - xAxis[0];
		double yR = yAxis[yAxis.length - 1] - yAxis[0];
		double xc = (double) aw / (xAxis.length - 1);
		double yc = (double) ah / (yAxis.length - 1);
		double xct = axesSize;
		double yct = ah + tRP;
		for (int i = 0; i < xAxis.length; i++) { // X-Axis Intervals
			g.drawLine((int) Math.round(xct), ah + tRP - barLines / 2, (int) Math.round(xct), ah + tRP + barLines / 2);
			gfu.DrawStringBounds(new Font(Font.SANS_SERIF, Font.PLAIN, 12), "" + xAxis[i], new Rectangle((int) Math.round(xct) - axesSize / 2, ah + tRP + barLines / 2, axesSize, barLines), GraphicsFontUtil.CENTER, false);
			if (i == xAxis.length - 1) continue;
			xct += (double) aw * (xAxis[i + 1] - xAxis[i]) / (double) xR;
		}
		for (int i = 0; i < yAxis.length; i++) { // Y-Axis Intervals
			g.drawLine(axesSize - barLines / 2, (int) Math.round(yct), axesSize + barLines / 2, (int) Math.round(yct));
			gfu.DrawStringBounds(new Font(Font.SANS_SERIF, Font.PLAIN, 12), "" + yAxis[i], new Rectangle(axesSize - barLines / 2 - (int) (barLines * 1.5), (int) Math.round(yct) - barLines / 2, axesSize, barLines), GraphicsFontUtil.LEFT, false);
			if (i == yAxis.length - 1) continue;
			yct -= (double) ah * (yAxis[i + 1] - yAxis[i]) / (double) yR;
		}
		// 2.4) Add Legend
		BufferedImage tImg2 = new BufferedImage(legendSpace, tImg.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = (Graphics2D) tImg2.getGraphics();
		GraphicsFontUtil gfu2 = new GraphicsFontUtil(g2);
		g2.setColor(Color.white);
		g2.fillRect(0, 0, tImg2.getWidth(), tImg2.getHeight());
		double lR = limits[1] - limits[0]; // Limit Range
		int intervals = 32;
		int labelInterval = 4;
		for (int i = 0; i < intervals; i++) {
			double tInt = 1 - (double) i / intervals;
			// int tr = (int) (255 * (tInt < 0.5 ? 0 : (tInt < 0.75 ? (tInt - 0.5) * 4.0 : 1)));
			// int tg = (int) (255 * (tInt < 0.75 ? (tInt < 0.25 ? tInt * 4.0 : 1) : (1 - tInt) * 4.0));
			// int tb = (int) (255 * (tInt < 0.5 ? (tInt < 0.25 ? 1 : 1 - (tInt - 0.25) * 4.0) : 0));
			g2.setColor(colorScheme.getColor(tInt));
			g2.fillRect((tImg2.getWidth() - legendSize) / 2, (int) (tRP + i * ((double) ah / intervals)), legendSize, (int) Math.round((double) ah / intervals));
			if (i % labelInterval == 0 || i == intervals - 1) {
				if (i == intervals - 1) tInt = 0;
				g2.setColor(Color.black);
				gfu2.DrawStringBounds(new Font(Font.SANS_SERIF, Font.PLAIN, 12), "" + (limits[0] + tInt * lR), //
						new Rectangle((tImg2.getWidth() - legendSize) / 2, (int) (tRP + i * ((double) ah / intervals)), legendSize, (int) Math.round((double) ah / intervals)), //
						GraphicsFontUtil.CENTER, false);
			}
		}

		// 2.?) Finalize Image
		img = new BufferedImage(tImg.getWidth() + tImg2.getWidth(), tImg.getHeight(), BufferedImage.TYPE_INT_ARGB);
		img.getGraphics().drawImage(tImg, 0, 0, null);
		img.getGraphics().drawImage(tImg2, tImg.getWidth(), 0, null);
		g.dispose();
		g2.dispose();

		// img.setRGB(0, 0, aw, ah, v, 0, 0);

		// int c = 0;
		// for (int i = 0; i < ah; i++) {
		// System.out.println();
		// for (int o = 0; o < aw; o++) {
		// System.out.printf("\t" + (int) (x[c++]));
		// }
		// }

		try {
			System.out.println("Filename: " + filename);
			File outputfile = new File(filename + ".png");
			ImageIO.write(img, "png", outputfile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
