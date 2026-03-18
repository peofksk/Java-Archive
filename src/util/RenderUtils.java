package util;

import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class RenderUtils {

    public static void drawCenteredString(Graphics2D g, String text, int x, int y, int width, int height) {
        FontMetrics fm = g.getFontMetrics();
        int drawX = x + (width - fm.stringWidth(text)) / 2;
        int drawY = y + ((height - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(text, drawX, drawY);
    }
}