package ver_2;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

public class LevelSelectState implements GameState {

	private final JAVA_Archive game;
	private final StageManager sm = new StageManager();
	private Image background, arrowLeft, arrowRight, pressEnter;

	private enum Mode {
		LEVEL_SELECT, DIFFICULTY_SELECT
	}

	private Mode mode = Mode.LEVEL_SELECT;

	public Difficulty difficulty = Difficulty.Easy;

	public LevelSelectState(JAVA_Archive game) {
		this.game = game;
	}

	private void playSample() {
		game.getContext().bgm.play(sm.getCurrentStage().getSamplePath(), true);
	}

	@Override
	public void enter() {
		AssetManager am = AssetManager.getInstance();
		background = am.getImage("selection_bg");
		arrowLeft = am.getImage("arrow_left");
		arrowRight = am.getImage("arrow_right");
		pressEnter = am.getImage("press_enter");

		playSample();
	}

	@Override
	public void update(double deltaTime) {
	}

	@Override
	public void render(Graphics2D g) {

		g.drawImage(background, 0, 0, null);

		Image titleImage = AssetManager.getInstance().getImage(sm.getCurrentStage().getTitleImageKey());
		g.drawImage(titleImage, 312, 80, null);

		if (mode == Mode.LEVEL_SELECT) {
			g.drawImage(pressEnter, 357, 500, null);
			if (sm.hasPrev())
				g.drawImage(arrowLeft, 77, 225, null);
			if (sm.hasPrev())
				g.drawImage(arrowRight, 713, 225, null);
		}

		if (mode == Mode.DIFFICULTY_SELECT) {
			g.setFont(new Font("SansSerif", Font.ITALIC, 35));
			g.setColor(Color.CYAN);
			g.drawString("Please set difficulty: ", 300, 530);
			g.setFont(new Font("SansSerif", Font.BOLD, 35));
			if (difficulty == Difficulty.Easy)
				g.setColor(Color.GREEN);
			else if (difficulty == Difficulty.Hard)
				g.setColor(Color.ORANGE);
			else if (difficulty == Difficulty.Extreme)
				g.setColor(Color.RED);
			g.drawString(difficulty.name(), 630, 530);
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (mode == Mode.LEVEL_SELECT) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				game.changeState(new IntroState(game));
				return;
			} else if (e.getKeyCode() == KeyEvent.VK_LEFT && sm.hasPrev()) {
				sm.prev();
				playSample();
			} else if (e.getKeyCode() == KeyEvent.VK_RIGHT && sm.hasNext()) {
				sm.next();
				playSample();
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				mode = Mode.DIFFICULTY_SELECT;
			}
		} else if (mode == Mode.DIFFICULTY_SELECT) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				mode = Mode.LEVEL_SELECT;
				difficulty = Difficulty.Easy;
			} else if (e.getKeyCode() == KeyEvent.VK_UP) {
				difficulty = difficulty.next();
			} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				difficulty = difficulty.prev();
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				game.changeState(new GamePlayState(game, sm.getCurrentStage(), difficulty));
			}
		}

	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {

		}
	}

	@Override
	public void exit() {
	}
}
