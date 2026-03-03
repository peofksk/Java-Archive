package state.gameplay;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import stage.Difficulty;
import stage.Stage;
import state.LevelSelectState;

public class GamePlayState implements GameState {

	private final GameContext context;
	private final AssetManager am = AssetManager.getInstance();
	private NoteManager noteManager;
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

	public GamePlayState(GameContext context, Stage stage, Difficulty difficulty) {
		this.context = context;
		this.stage = stage;
		this.difficulty = difficulty;
		background = am.getImage(stage.getBackgroundImageKey());
	}

	@Override
	public void enter() {
		startMusic();
		noteManager = new NoteManager();
		noteManager.addNote(new Note(Lane.D, 2.0));
		noteManager.addNote(new Note(Lane.F, 3.0));
		noteManager.addNote(new Note(Lane.J, 4.0));
		noteManager.addNote(new Note(Lane.K, 5.0));
		started = true;
	}

	@Override
	public void exit() {
		context.bgm.stop();
	}

	@Override
	public void update(double deltaTime) {

		if (!started || paused || gameOver)
			return;

		elapsedTime += deltaTime;
		noteManager.update(elapsedTime);
		System.out.println("update: " + deltaTime);
		System.out.println("started=" + started + ", paused=" + paused + ", gameOver=" + gameOver);

	}

	@Override
	public void render(Graphics2D g) {
		g.drawImage(background, 0, 0, null);

		int baseY = 450;
		int laneWidth = 80;
		int startX = 300;
		double speed = 300;

		for (Lane lane : Lane.values()) {

			int laneIndex = lane.ordinal();
			int x = startX + laneIndex * laneWidth;

			for (Note note : noteManager.getLaneNotes().get(lane)) {

				double y = baseY - (note.getHitTime() - elapsedTime) * speed;

				g.fillRect(x, (int) y, 60, 20);
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (gameOver) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				context.changeState(new LevelSelectState(context));
			}
			return;
		}

		if (paused) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				context.changeState(new LevelSelectState(context));
			}
			if (e.getKeyCode() == KeyEvent.VK_P) {
				paused = false;
			}
			return;
		}

		if (!started)
			return;

		if (e.getKeyCode() == KeyEvent.VK_P) {
			paused = true;
		} else if (e.getKeyCode() == KeyEvent.VK_D) {
			noteManager.judge(Lane.D, elapsedTime);
		} else if (e.getKeyCode() == KeyEvent.VK_F) {
			noteManager.judge(Lane.F, elapsedTime);
		} else if (e.getKeyCode() == KeyEvent.VK_J) {
			noteManager.judge(Lane.J, elapsedTime);
		} else if (e.getKeyCode() == KeyEvent.VK_K) {
			noteManager.judge(Lane.K, elapsedTime);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	private void startMusic() {
		context.bgm.play(stage.getMusicPath(), false);
	}

}