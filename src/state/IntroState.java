package state;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

import asset.AssetManager;
import core.GameContext;
import core.GameState;

public class IntroState implements GameState {

	private final GameContext context;
	private final AssetManager am = AssetManager.getInstance();
	private Image background, title, pressEnter;

	public IntroState(GameContext context) {
		this.context = context;
	}

	@Override
	public void enter() {
		background = am.getImage("intro_bg");
		title = am.getImage("game_title");
		pressEnter = am.getImage("press_enter");

		context.bgm.play("/introMusic.wav", true);
	}

	@Override
	public void update(double deltaTime) {
	}

	@Override
	public void render(Graphics2D g) {
		g.drawImage(background, 0, 0, null);
		g.drawImage(title, 172, 121, null);
		g.drawImage(pressEnter, 357, 450, null);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			System.exit(0);
		}
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			context.changeState(new LevelSelectState(context));
		}
	}

	@Override
	public void exit() {
		context.bgm.stop();
	}
}
