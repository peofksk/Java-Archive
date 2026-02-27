package ver_2;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

public class LevelSelectState implements GameState {

	private final JAVA_Archive game;
	private Image background, arrowLeft, arrowRight;
	private Image[] titles;
	private enum Mode {
		LEVEL_SELECT, DIFFICULTY_SELECT
	}
	public enum Difficulty {
	    Easy,
	    Hard,
	    Extreme;

	    public Difficulty next() {
	        int next = ordinal() + 1;
	        if (next >= values().length) {
	            next = values().length - 1;
	        }
	        return values()[next];
	    }

	    public Difficulty prev() {
	        int prev = ordinal() - 1;
	        if (prev < 0) {
	            prev = 0;
	        }
	        return values()[prev];
	    }
	}
	private Mode mode = Mode.LEVEL_SELECT;
	private int index = 0;
	private Difficulty difficulty = Difficulty.Easy;
	
	private final String[] samples = { "/sample_unwelcomeSchool.wav", "/sample_afterSchoolDessert.wav",
			"/sample_comingSoon.wav" };

	public LevelSelectState(JAVA_Archive game) {
		this.game = game;
	}

	private void playSample() {
		game.getContext().bgm.play(samples[index], true);
	}

	@Override
	public void enter() {
		AssetManager am = AssetManager.getInstance();
		background = am.getImage("selection_bg");
		arrowLeft = am.getImage("arrow_left");
		arrowRight = am.getImage("arrow_right");

		titles = new Image[] { am.getImage("title_unwelcome"), am.getImage("title_after"),
				am.getImage("title_coming") };

		index = 0;
		playSample();
	}

	@Override
	public void update(double deltaTime) {
	}

	@Override
	public void render(Graphics2D g) {

		g.drawImage(background, 0, 0, null);
		g.drawImage(titles[index], 312, 80, null);

		if (mode == Mode.LEVEL_SELECT) {
			if (index > 0)
				g.drawImage(arrowLeft, 77, 225, null);
			if (index < titles.length - 1)
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
			}
			if (e.getKeyCode() == KeyEvent.VK_LEFT && index > 0) {
				index--;
				playSample();
			}
			if (e.getKeyCode() == KeyEvent.VK_RIGHT && index < titles.length - 1) {
				index++;
				playSample();
			}
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				mode = Mode.DIFFICULTY_SELECT;
			}
		}
		if (mode == Mode.DIFFICULTY_SELECT) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				mode = Mode.LEVEL_SELECT;
			}
			if (e.getKeyCode() == KeyEvent.VK_UP) {
				difficulty = difficulty.next();
			}
			if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				difficulty = difficulty.prev();
			}
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				// Game Start
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
