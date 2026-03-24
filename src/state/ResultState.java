package state;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import state.gameplay.GameResult;
import state.gameplay.Judgement;

public class ResultState implements GameState {

    private final GameContext context;
    private final AssetManager am = AssetManager.getInstance();

    private Image background;

    private boolean preloaded = false;

    private final GameResult result;

    public ResultState(GameContext context, GameResult result) {
        this.context = context;
        this.result = result;
    }

    public void preload() {
        if (preloaded) {
            return;
        }

        background = am.getImage("result_bg");
        context.bgm.load("/resultMusic.wav");

        preloaded = true;
    }

    @Override
    public void enter() {
        context.bgm.stop();

        if (!preloaded) {
            preload();
        }
        context.bgm.playLoaded(true);
    }

    @Override
    public void update(double deltaTime) {

    }

    @Override
    public void render(Graphics2D g) {
        g.drawImage(background, 0, 0, null);

        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.setColor(Color.GRAY);

        g.drawString("SCORE : ", 150, 250);
        g.drawString(String.valueOf(result.getScore()), 320, 250);

        g.drawString("MAX COMBO : ", 150, 300);
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

    @Override
    public void exit() {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            context.changeState(new LevelSelectState(context));
        }
    }
}