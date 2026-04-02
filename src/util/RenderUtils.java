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

	public static void drawOutlinedCenteredString(Graphics2D g, String text, int centerX, int y, Color fillColor,
			Color outlineColor) {
		FontMetrics metrics = g.getFontMetrics(g.getFont());
		int textWidth = metrics.stringWidth(text);
		int startX = centerX - (textWidth / 2);

		g.setColor(outlineColor);
		g.drawString(text, startX - 1, y);
		g.drawString(text, startX + 1, y);
		g.drawString(text, startX, y - 1);
		g.drawString(text, startX, y + 1);

		g.setColor(fillColor);
		g.drawString(text, startX, y);
	}
}