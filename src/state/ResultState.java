package state;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

import asset.AssetManager;
import core.GameContext;
import core.GameState;

public class ResultState implements GameState {

	private final GameContext context;
	private final AssetManager am = AssetManager.getInstance();
	
	private Image background;
	
	private boolean preloaded = false;
	
	private final int score;
	private final int maxCombo;
	private final double accuracy;
	
	
	
	public ResultState(GameContext context, int score, int maxCombo, double accuracy) {
		this.context = context;
		this.score = score;
		this.maxCombo = maxCombo;
		this.accuracy = accuracy;
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
		
	}

	@Override
	public void update(double deltaTime) {
		
	}

	@Override
	public void render(Graphics2D g) {
		g.drawImage(background, 0, 0, null);
		
		g.setFont(new Font("Arial", Font.BOLD, 50));
        g.setColor(Color.CYAN);

        g.drawString("SCORE : ", 240, 250);
        g.drawString(String.valueOf(score), 400, 250);
        g.drawString("MAX COMBO : ", 240, 280);

        g.drawString(String.valueOf(maxCombo), 400, 280);

        g.drawString("Accuracy : ", 240, 340);
        g.drawString(String.format("%.2f", accuracy), 410, 340);

        Graphics2D g2 = (Graphics2D) g.create();
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
