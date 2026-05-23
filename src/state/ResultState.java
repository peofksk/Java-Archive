package state;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import state.gameplay.GameResult;
import state.gameplay.Judgement;

public class ResultState implements GameState {

    private static final int BACK_BUTTON_W = 96;
    private static final int BACK_BUTTON_H = 36;
    private static final int BACK_BUTTON_X = 1024 - BACK_BUTTON_W - 36;
    private static final int BACK_BUTTON_Y = 34;

    private final GameContext context;
    private final AssetManager am = AssetManager.getInstance();

    private Image background;
    private boolean preloaded = false;

    private final GameResult result;

    private boolean backButtonHovered = false;
    private boolean backButtonPressed = false;

    public ResultState(GameContext context, GameResult result) {
        this.context = context;
        this.result = result;
    }

    public void preload() {
        if (preloaded) {
            return;
        }

        background = am.getImage("result_bg");
        context.bgm.load("/audio/bgm/resultMusic.wav");

        preloaded = true;
    }

    @Override
    public void enter() {
        context.bgm.stop();

        if (!preloaded) {
            preload();
        }

        backButtonHovered = false;
        backButtonPressed = false;

        context.bgm.playLoaded(true);
    }

    @Override
    public void update(double deltaTime) {
    }

    @Override
    public void render(Graphics2D g) {
        if (background != null) {
            g.drawImage(background, 0, 0, null);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 1024, 576);
        }

        drawBackButton(g);
        drawResultText(g);
    }

    private void drawResultText(Graphics2D g) {
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.setColor(Color.GRAY);

        g.drawString("Score : ", 150, 250);
        g.drawString(String.valueOf(result.getScore()), 320, 250);

        g.drawString("Max Combo : ", 150, 300);
        g.drawString(String.valueOf(result.getMaxCombo()), 380, 300);

        g.drawString("Accuracy : ", 150, 350);
        g.drawString(String.format("%.2f", result.getAccuracy()), 320, 350);

        int rightLabelX = 440;
        int rightValueX = 590;
        int startY = 250;
        int gapY = 45;

        g.setFont(new Font("Arial", Font.BOLD, 28));

        g.setColor(Color.BLUE);
        g.drawString("Perfect : ", rightLabelX, startY);
        g.drawString(String.valueOf(result.getJudgementCount(Judgement.PERFECT)), rightValueX, startY);

        g.setColor(Color.CYAN);
        g.drawString("Great : ", rightLabelX, startY + gapY);
        g.drawString(String.valueOf(result.getJudgementCount(Judgement.GREAT)), rightValueX, startY + gapY);

        g.setColor(Color.GREEN);
        g.drawString("Good : ", rightLabelX, startY + gapY * 2);
        g.drawString(String.valueOf(result.getJudgementCount(Judgement.GOOD)), rightValueX, startY + gapY * 2);

        g.setColor(Color.ORANGE);
        g.drawString("Early : ", rightLabelX, startY + gapY * 3);
        g.drawString(String.valueOf(result.getJudgementCount(Judgement.EARLY)), rightValueX, startY + gapY * 3);

        g.setColor(Color.ORANGE);
        g.drawString("Late : ", rightLabelX, startY + gapY * 4);
        g.drawString(String.valueOf(result.getJudgementCount(Judgement.LATE)), rightValueX, startY + gapY * 4);

        g.setColor(Color.GRAY);
        g.drawString("Miss : ", rightLabelX, startY + gapY * 5);
        g.drawString(String.valueOf(result.getJudgementCount(Judgement.MISS)), rightValueX, startY + gapY * 5);
    }

    private void drawBackButton(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle bounds = getBackButtonBounds();

            Color fill;
            Color border;
            Color text;

            if (backButtonPressed) {
                fill = new Color(35, 105, 165, 230);
                border = new Color(210, 245, 255, 255);
                text = Color.WHITE;
            } else if (backButtonHovered) {
                fill = new Color(70, 145, 205, 210);
                border = new Color(190, 235, 255, 245);
                text = Color.WHITE;
            } else {
                fill = new Color(0, 0, 0, 135);
                border = new Color(130, 190, 230, 210);
                text = new Color(235, 248, 255);
            }

            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(bounds.x + 3, bounds.y + 4, bounds.width, bounds.height, 14, 14);

            g2.setColor(fill);
            g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);

            g2.setStroke(new BasicStroke(2f));
            g2.setColor(border);
            g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);

            g2.setFont(new Font("Arial", Font.BOLD, 16));
            drawCenteredString(g2, "BACK", bounds, text);

        } finally {
            g2.dispose();
        }
    }

    private void drawCenteredString(Graphics2D g, String text, Rectangle bounds, Color color) {
        FontMetrics fm = g.getFontMetrics();

        int textX = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
        int textY = bounds.y + ((bounds.height - fm.getHeight()) / 2) + fm.getAscent();

        g.setColor(new Color(0, 0, 0, 130));
        g.drawString(text, textX + 1, textY + 1);

        g.setColor(color);
        g.drawString(text, textX, textY);
    }

    private Rectangle getBackButtonBounds() {
        return new Rectangle(BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_W, BACK_BUTTON_H);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER) {
            backToLevelSelect();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        backButtonHovered = getBackButtonBounds().contains(e.getPoint());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        backButtonPressed = getBackButtonBounds().contains(e.getPoint());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            backButtonPressed = false;
            return;
        }

        Point point = e.getPoint();

        if (backButtonPressed && getBackButtonBounds().contains(point)) {
            backButtonPressed = false;
            backToLevelSelect();
            return;
        }

        backButtonPressed = false;
    }

    private void backToLevelSelect() {
        context.changeState(new LevelSelectState(context));
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void exit() {
        backButtonHovered = false;
        backButtonPressed = false;
    }
}