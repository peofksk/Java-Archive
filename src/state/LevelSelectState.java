package state;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import stage.Difficulty;
import state.gameplay.GamePlayState;

public class LevelSelectState implements GameState {

	private final GameContext context;
	private final AssetManager am = AssetManager.getInstance();
	private Image background, arrowLeft, arrowRight, pressEnter;

	private enum Mode {
		LEVEL_SELECT, DIFFICULTY_SELECT
	}

	private Mode mode = Mode.LEVEL_SELECT;

	public LevelSelectState(GameContext context) {
		this.context = context;
	}

	private void playSample() {
		context.bgm.play(context.sm.getCurrentStage().getSamplePath(), true);
	}

	@Override
	public void enter() {
		background = am.getImage("selection_bg");
		arrowLeft = am.getImage("arrow_left");
		arrowRight = am.getImage("arrow_right");
		pressEnter = am.getImage("press_enter");

		context.setCurrentDifficulty(Difficulty.Easy);

		playSample();
	}

	@Override
	public void update(double deltaTime) {
	}

	@Override
	public void render(Graphics2D g) {

		g.drawImage(background, 0, 0, null);

		Image titleImage = AssetManager.getInstance().getImage(context.sm.getCurrentStage().getTitleImageKey());
		g.drawImage(titleImage, 312, 80, null);

		if (mode == Mode.LEVEL_SELECT) {
			g.drawImage(pressEnter, 357, 500, null);
			if (context.sm.hasPrev())
				g.drawImage(arrowLeft, 77, 225, null);
			if (context.sm.hasNext())
				g.drawImage(arrowRight, 713, 225, null);
		}

		if (mode == Mode.DIFFICULTY_SELECT) {
			g.setFont(new Font("SansSerif", Font.ITALIC, 35));
			g.setColor(Color.CYAN);
			g.drawString("Please set difficulty: ", 300, 530);
			g.setFont(new Font("SansSerif", Font.BOLD, 35));
			if (context.getCurrentDifficulty() == Difficulty.Easy)
				g.setColor(Color.GREEN);
			else if (context.getCurrentDifficulty() == Difficulty.Hard)
				g.setColor(Color.ORANGE);
			else if (context.getCurrentDifficulty() == Difficulty.Extreme)
				g.setColor(Color.RED);
			g.drawString(context.getCurrentDifficulty().name(), 630, 530);
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (mode == Mode.LEVEL_SELECT) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				context.changeState(new IntroState(context));
				return;
			} else if (e.getKeyCode() == KeyEvent.VK_C) {
				CalibrationState next = new CalibrationState(context, context.sm.getCurrentCorrectionConfig());
				context.changeState(new LoadState(context, "Loading Calibration...", next::preload, () -> next));
			} else if (e.getKeyCode() == KeyEvent.VK_O) {
				context.changeState(new OptionState(context));
			}
			
			else if (e.getKeyCode() == KeyEvent.VK_S) {
				context.changeState(new ShopState(context));
			} else if (e.getKeyCode() == KeyEvent.VK_LEFT && context.sm.hasPrev()) {
				context.sm.prev();
				playSample();
			} else if (e.getKeyCode() == KeyEvent.VK_RIGHT && context.sm.hasNext()) {
				context.sm.next();
				playSample();
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				mode = Mode.DIFFICULTY_SELECT;
			}
		} else if (mode == Mode.DIFFICULTY_SELECT) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				mode = Mode.LEVEL_SELECT;
				context.setCurrentDifficulty(context.getCurrentDifficulty().initialize());
			} else if (e.getKeyCode() == KeyEvent.VK_UP) {
				context.setCurrentDifficulty(context.getCurrentDifficulty().next());
			} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				context.setCurrentDifficulty(context.getCurrentDifficulty().prev());
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				GamePlayState next = new GamePlayState(context, context.sm.getCurrentStage(),
						context.getCurrentDifficulty());

				context.changeState(new LoadState(context, "Loading Game...", () -> next.preload(), () -> next));
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