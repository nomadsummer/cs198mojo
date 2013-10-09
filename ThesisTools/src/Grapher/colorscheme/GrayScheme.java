package Grapher.colorscheme;

import java.awt.Color;

public class GrayScheme extends ColorScheme {
	public Color getColor(double t) {
		return new Color((int) (t * 255), (int) (t * 255), (int) (t * 255));
	}
}
