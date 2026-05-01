package state;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import util.RenderUtils;

public class OptionState implements GameState {

	private static final int COLS = 3;
	private static final int ROWS = 4;

	private static final int PANEL_X = 390;
	private static final int PANEL_Y = 70;
	private static final int PANEL_W = 470;

	private static final int TITLE_Y = PANEL_Y + 18;

	private static final int GRID_X = PANEL_X + 38;
	private static final int GRID_Y = PANEL_Y + 70;
	private static final int CELL_W = 132;
	private static final int CELL_H = 64;

	private static final int CURRENT_Y = PANEL_Y + 335;
	private static final int GUIDE_Y = PANEL_Y + 382;

	private final GameContext context;
	private final AssetManager am = AssetManager.getInstance();

	private Image background;
	private Image[] notes;

	private boolean preloaded = false;

	public OptionState(GameContext context) {
		this.context = context;
	}

	private void preload() {
		if (preloaded)
			return;

		context.bgm.load("/optionMusic.wav");
		background = am.getImage("option_bg");
		notes = new Image[context.getNoteCount()];

		for (int i = 0; i < context.getNoteCount(); i++) {
			notes[i] = am.getImage("note_" + i);
		}

		preloaded = true;
	}

	@Override
	public void enter() {
		context.bgm.stop();

		if (!preloaded)
			preload();

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

		Color titleColor = new Color(102, 88, 74);
		Color textColor = new Color(116, 102, 88);
		Color selectedTextColor = new Color(88, 164, 203);

		g.setColor(titleColor);
		g.setFont(new Font("SansSerif", Font.BOLD, 28));
		RenderUtils.drawCenteredString(g, "Select Note Skin", PANEL_X, TITLE_Y, PANEL_W, 32);

		for (int i = 0; i < context.getNoteCount(); i++) {
			int col = i % COLS;
			int row = i / COLS;

			int cellX = GRID_X + col * CELL_W;
			int cellY = GRID_Y + row * CELL_H;

			Image note = notes[i];
			int drawX = cellX;
			int drawY = cellY;

			if (note != null) {
				int noteW = note.getWidth(null);
				int noteH = note.getHeight(null);

				if (noteW > 0 && noteH > 0) {
					drawX = cellX + (CELL_W - noteW) / 2;
					drawY = cellY + 2;
				}

				g.drawImage(note, drawX, drawY, null);
			}

			if (i == context.getNoteIndex()) {
				g.setColor(selectedTextColor);
				g.drawRoundRect(cellX + 18, cellY - 4, CELL_W - 36, 48, 14, 14);

			}

			g.setFont(new Font("SansSerif", i == context.getNoteIndex() ? Font.BOLD : Font.PLAIN, 13));
			g.setColor(i == context.getNoteIndex() ? selectedTextColor : textColor);
			RenderUtils.drawCenteredString(g, "note_" + i, cellX, cellY + 38, CELL_W, 18);
		}

		g.setColor(titleColor);
		g.setFont(new Font("SansSerif", Font.BOLD, 18));
		RenderUtils.drawCenteredString(g, "Current : note_" + context.getNoteIndex(), PANEL_X, CURRENT_Y, PANEL_W, 24);

		g.setColor(textColor);
		g.setFont(new Font("SansSerif", Font.PLAIN, 14));
		RenderUtils.drawCenteredString(g, "Arrow Keys : Move    Enter : Apply    ESC : Back", PANEL_X, GUIDE_Y, PANEL_W,
				18);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int current = context.getNoteIndex();
		int noteCount = context.getNoteCount();

		int row = current / COLS;
		int col = current % COLS;

		switch (e.getKeyCode()) {
		case KeyEvent.VK_LEFT -> {
			if (col > 0) {
				context.setNoteIndex(current - 1);
			}
		}
		case KeyEvent.VK_RIGHT -> {
			if (col < COLS - 1) {
				int next = current + 1;
				if (next < noteCount) {
					context.setNoteIndex(next);
				}
			}
		}
		case KeyEvent.VK_UP -> {
			if (row > 0) {
				context.setNoteIndex(current - COLS);
			}
		}
		case KeyEvent.VK_DOWN -> {
			if (row < ROWS - 1) {
				int next = current + COLS;
				if (next < noteCount) {
					context.setNoteIndex(next);
				}
			}
		}
		case KeyEvent.VK_ESCAPE -> context.changeState(new LevelSelectState(context));
		case KeyEvent.VK_ENTER -> context.changeState(new LevelSelectState(context));
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void exit() {
		context.saveSettings();
	}
}