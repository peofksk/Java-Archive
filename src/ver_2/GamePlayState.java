package ver_2;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

public class GamePlayState implements GameState {

	private final JAVA_Archive game;

	private final Level level;
	private final Difficulty difficulty;

	private boolean started = false;
	private boolean paused = false;
	private boolean gameOver = false;

	private double elapsedTime = 0;
	private double musicStartTime = 0;

	private int score = 0;
	private int combo = 0;
	private int maxCombo = 0;

	public GamePlayState(JAVA_Archive game, Level level, Difficulty difficulty) {
		this.game = game;
		this.level = level;
		this.difficulty = difficulty;
	}

	@Override
    public void enter() {
    	AssetManager am = AssetManager.getInstance();
    	Image background = am.getImage("");
    	startMusic();
        started = true;
    }

	@Override
	public void exit() {
		game.getContext().bgm.stop();
	}

	@Override
	public void update(double deltaTime) {

		if (!started || paused || gameOver)
			return;

		elapsedTime += deltaTime;

	}

	@Override
	public void render(Graphics2D g) {

	}

	@Override
	public void keyPressed(KeyEvent e) {

		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			game.changeState(new LevelSelectState(game));
			return;
		}

		if (e.getKeyCode() == KeyEvent.VK_P) {
			paused = !paused;
		}

		if (paused || gameOver)
			return;
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	private void startMusic() {
		game.getContext().bgm.play(level.getMusicPath(), false);
	}

}