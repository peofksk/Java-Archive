package util;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class RenderUtils {

	private RenderUtils() {

	}

	public static void drawCenteredString(Graphics2D g, String text, int x, int y, int width, int height) {
		FontMetrics fm = g.getFontMetrics();
		int drawX = x + (width - fm.stringWidth(text)) / 2;
		int drawY = y + ((height - fm.getHeight()) / 2) + fm.getAscent();
		g.drawString(text, drawX, drawY);
	}

	public static void drawOutlinedCenteredString(Graphics2D g, String text, int x, int y, int width, int height,
	        Color fillColor, Color outlineColor) {
	    FontMetrics fm = g.getFontMetrics();
	    int drawX = x + (width - fm.stringWidth(text)) / 2;
	    int drawY = y + ((height - fm.getHeight()) / 2) + fm.getAscent();

	    Color oldColor = g.getColor();

	    g.setColor(outlineColor);
	    g.drawString(text, drawX - 1, drawY);
	    g.drawString(text, drawX + 1, drawY);
	    g.drawString(text, drawX, drawY - 1);
	    g.drawString(text, drawX, drawY + 1);

	    g.setColor(fillColor);
	    g.drawString(text, drawX, drawY);

	    g.setColor(oldColor);
	}
}