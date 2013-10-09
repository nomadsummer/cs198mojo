package Grapher;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class GraphicsFontUtil {

	public static final int LEFT = 1;
	public static final int CENTER = 2;
	public static final int RIGHT = 4;
	public static final int TOP = 8;
	public static final int MIDDLE = 16;
	public static final int BOTTOM = 32;

	protected Graphics2D g;

	public GraphicsFontUtil(Graphics2D g) {
		this.g = g;
		// g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	public void DrawStringBounds(Font f, String s, Rectangle r) {
		DrawStringBounds(f, s, r, LEFT, true);
	}

	public void DrawStringBounds(Font f, String s, Rectangle r, int align, boolean outline) {
		if (outline) g.drawRect(r.x, r.y, r.width, r.height);

		Font newFont = new Font(f.getName(), f.getStyle(), f.getSize());
		g.setFont(newFont);
		FontMetrics fm = g.getFontMetrics();
		// int sideTextPadding = fm.stringWidth(" ");

		int i = 1;
		while (getStringWidth(s, fm) > r.width) {
			newFont = new Font(f.getName(), f.getStyle(), f.getSize() - i);
			g.setFont(newFont);
			fm = g.getFontMetrics();
			i++;
		}

		int strWidth = fm.stringWidth(s);

		int x = r.x;
		int padding = getPadding(fm);
		if (align == CENTER) x += (r.width - strWidth) / 2;
		else if (align == LEFT) x += padding;
		else if (align == RIGHT) x += r.width - (strWidth + padding);
		int y = r.y + r.height - fm.getMaxDescent();
		g.drawString(s, x, y);
	}

	public void DrawTextAreaBounds(Font f, String[] str, Rectangle r, boolean outline) {
		List<String> lines = new ArrayList<String>();

		int[] parLines = new int[str.length];
		String[][] words = new String[str.length][];
		for (int i = 0; i < str.length; i++)
			words[i] = str[i].split(" ");

		Font newFont;
		FontMetrics fm;
		int paragraphPadding;
		int totalHeight;
		int fSize = f.getSize();

		do {
			newFont = new Font(f.getName(), f.getStyle(), fSize);
			g.setFont(newFont);
			fm = g.getFontMetrics();

			lines.clear();

			for (int parCount = 0; parCount < str.length; parCount++) {
				int x = 0;
				parLines[parCount] = 0;
				while (x < words[parCount].length) {
					String s = words[parCount][x];
					x++;
					while (x < words[parCount].length && getStringWidth(s + " " + words[parCount][x], fm) <= r.width) {
						s += " " + words[parCount][x];
						x++;
					}
					lines.add(s);
					parLines[parCount]++;
				}
			}

			fSize--;
			paragraphPadding = (int) (fm.getHeight() * 0.2f);
			totalHeight = lines.size() * fm.getHeight() + paragraphPadding * (str.length - 1);
		} while (totalHeight > r.height);

		int fontHeight = fm.getHeight();
		int y = r.y;
		int parIndex = 0;
		int parLineLimit = parLines.length > 0 ? parLines[0] : 0;
		for (int i = 0; i < lines.size(); i++) {
			if (i >= parLineLimit) {
				parIndex++;
				parLineLimit += parLines[parIndex];
				y += paragraphPadding;
			}

			DrawStringBounds(newFont, lines.get(i), new Rectangle(r.x, y, r.width, fontHeight), LEFT, false);
			// System.out.println(">>" + lines.get(i) + "<<");
			y += fontHeight;
		}
		if (outline) {
			g.drawRect(r.x, r.y, r.width, totalHeight);
		}
	}

	public static int getStringWidth(String s, FontMetrics fm) {
		return fm.stringWidth(s) + fm.stringWidth(" ");
	}

	public static int getPadding(FontMetrics fm) {
		return fm.stringWidth(" ");
	}
}
