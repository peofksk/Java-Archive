package ver_2;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

public class GamePlayState implements GameState {

	private final JAVA_Archive game;
	private final AssetManager am = AssetManager.getInstance();
	private final Stage stage;
	private final Difficulty difficulty;

	private Image background;

	private boolean started = false;
	private boolean paused = false;
	private boolean gameOver = false;

	private double elapsedTime = 0;
	private double musicStartTime = 0;

	private int score = 0;
	private int combo = 0;
	private int maxCombo = 0;

	public GamePlayState(JAVA_Archive game, Stage stage, Difficulty difficulty) {
		this.game = game;
		this.stage = stage;
		this.difficulty = difficulty;
		background = am.getImage(stage.getBackgroundImageKey());
	}

	@Override
	public void enter() {

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
		g.drawImage(background, 0, 0, null);
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
		game.getContext().bgm.play(stage.getMusicPath(), false);
	}

}