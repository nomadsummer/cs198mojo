package grapher.colorscheme;

import java.awt.Color;

public class HeatScheme extends ColorScheme {
	public Color getColor(double t) {
		int r = (int) (255 * (t < 0.5 ? 0 : (t < 0.75 ? (t - 0.5) * 4.0 : 1)));
		int g = (int) (255 * (t < 0.75 ? (t < 0.25 ? t * 4.0 : 1) : (1 - t) * 4.0));
		int b = (int) (255 * (t < 0.5 ? (t < 0.25 ? 1 : 1 - (t - 0.25) * 4.0) : 0));

		return new Color(r, g, b);
	}
}
