package state;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import util.RenderUtils;

public class ShopState implements GameState {

	private static final int COLS = 3;
	private static final int ROWS = 4;

	private static final int SCREEN_W = 1024;

	private static final int PANEL_W = 540;
	private static final int PANEL_H = 420;
	private static final int PANEL_X = (SCREEN_W - PANEL_W) / 2 + 150;
	private static final int PANEL_Y = 80;

	private static final int TITLE_Y = PANEL_Y + 18;

	private static final int CELL_W = 132;
	private static final int CELL_H = 64;
	private static final int GRID_W = COLS * CELL_W;
	private static final int GRID_X = PANEL_X + (PANEL_W - GRID_W) / 2;
	private static final int GRID_Y = PANEL_Y + 70;

	private static final int CURRENT_Y = PANEL_Y + 335;
	private static final int GUIDE_Y = PANEL_Y + 375;

	private static final int BACK_W = 72;
	private static final int BACK_H = 28;
	private static final int BACK_X = PANEL_X + PANEL_W - BACK_W - 40;
	private static final int BACK_Y = PANEL_Y + 18;

	private final GameContext context;
	private final AssetManager am = AssetManager.getInstance();

	private Image background;
	private Image[] notes;

	private boolean preloaded = false;
	private int hoveredIndex = -1;
	private boolean backHovered = false;

	public ShopState(GameContext context) {
		this.context = context;
	}

	private void preload() {
		if (preloaded) {
			return;
		}

		context.bgm.load("/audio/bgm/optionMusic.wav");

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
		if (background != null) {
			g.drawImage(background, 0, 0, null);
		} else {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, 1024, 576);
		}

		Color titleColor = new Color(102, 88, 74);
		Color textColor = new Color(116, 102, 88);
		Color selectedTextColor = new Color(88, 164, 203);
		Color hoverColor = new Color(160, 190, 210);
		Color backColor = backHovered ? new Color(88, 164, 203) : textColor;

		g.setColor(titleColor);
		g.setFont(new Font("SansSerif", Font.BOLD, 28));
		RenderUtils.drawCenteredString(g, "Select Note Skin", PANEL_X, TITLE_Y, PANEL_W, 32);

		drawBackButton(g, backColor);

		for (int i = 0; i < context.getNoteCount(); i++) {
			drawNoteCell(g, i, textColor, selectedTextColor, hoverColor);
		}

		g.setColor(titleColor);
		g.setFont(new Font("SansSerif", Font.BOLD, 18));
		RenderUtils.drawCenteredString(g, "Current : note_" + context.getNoteIndex(), PANEL_X, CURRENT_Y, PANEL_W, 24);

		g.setColor(textColor);
		g.setFont(new Font("SansSerif", Font.PLAIN, 14));
		RenderUtils.drawCenteredString(
				g,
				"Mouse : Select / Wheel : Change / ESC or Right Click : Back",
				PANEL_X,
				GUIDE_Y,
				PANEL_W,
				18
		);
	}

	private void drawBackButton(Graphics2D g, Color color) {
		g.setColor(color);
		g.drawRoundRect(BACK_X, BACK_Y, BACK_W, BACK_H, 12, 12);

		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		RenderUtils.drawCenteredString(g, "BACK", BACK_X, BACK_Y + 4, BACK_W, 18);
	}

	private void drawNoteCell(
			Graphics2D g,
			int index,
			Color textColor,
			Color selectedTextColor,
			Color hoverColor
	) {
		Rectangle cell = getCellBounds(index);

		boolean selected = index == context.getNoteIndex();
		boolean hovered = index == hoveredIndex;

		if (hovered && !selected) {
			g.setColor(new Color(255, 255, 255, 70));
			g.fillRoundRect(cell.x + 14, cell.y - 6, cell.width - 28, 52, 14, 14);
			g.setColor(hoverColor);
			g.drawRoundRect(cell.x + 14, cell.y - 6, cell.width - 28, 52, 14, 14);
		}

		Image note = notes[index];

		if (note != null) {
			int noteW = note.getWidth(null);
			int noteH = note.getHeight(null);

			int drawX = cell.x;
			int drawY = cell.y;

			if (noteW > 0 && noteH > 0) {
				drawX = cell.x + (CELL_W - noteW) / 2;
				drawY = cell.y + 2;
			}

			g.drawImage(note, drawX, drawY, null);
		}

		if (selected) {
			g.setColor(selectedTextColor);
			g.drawRoundRect(cell.x + 18, cell.y - 4, CELL_W - 36, 48, 14, 14);
		}

		g.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 13));
		g.setColor(selected ? selectedTextColor : textColor);
		RenderUtils.drawCenteredString(g, "note_" + index, cell.x, cell.y + 38, CELL_W, 18);
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
					setNoteIndex(current - 1);
				}
			}
			case KeyEvent.VK_RIGHT -> {
				if (col < COLS - 1) {
					int next = current + 1;
					if (next < noteCount) {
						setNoteIndex(next);
					}
				}
			}
			case KeyEvent.VK_UP -> {
				if (row > 0) {
					setNoteIndex(current - COLS);
				}
			}
			case KeyEvent.VK_DOWN -> {
				if (row < ROWS - 1) {
					int next = current + COLS;
					if (next < noteCount) {
						setNoteIndex(next);
					}
				}
			}
			case KeyEvent.VK_ESCAPE, KeyEvent.VK_ENTER -> backToLevelSelect();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON3) {
			backToLevelSelect();
			return;
		}

		if (e.getButton() != MouseEvent.BUTTON1) {
			return;
		}

		Point point = e.getPoint();

		if (isBackButtonAt(point.x, point.y)) {
			backToLevelSelect();
			return;
		}

		int index = getNoteIndexAt(point.x, point.y);
		if (index >= 0) {
			setNoteIndex(index);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		Point point = e.getPoint();

		hoveredIndex = getNoteIndexAt(point.x, point.y);
		backHovered = isBackButtonAt(point.x, point.y);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		mouseMoved(e);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int rotation = e.getWheelRotation();

		if (rotation > 0) {
			selectNextNote();
		} else if (rotation < 0) {
			selectPreviousNote();
		}
	}

	private void selectPreviousNote() {
		int current = context.getNoteIndex();

		if (current > 0) {
			setNoteIndex(current - 1);
		}
	}

	private void selectNextNote() {
		int current = context.getNoteIndex();
		int next = current + 1;

		if (next < context.getNoteCount()) {
			setNoteIndex(next);
		}
	}

	private void setNoteIndex(int index) {
		if (index < 0 || index >= context.getNoteCount()) {
			return;
		}

		context.setNoteIndex(index);
	}

	private int getNoteIndexAt(int x, int y) {
		for (int i = 0; i < context.getNoteCount(); i++) {
			Rectangle cell = getCellBounds(i);

			Rectangle clickableArea = new Rectangle(
					cell.x + 10,
					cell.y - 8,
					cell.width - 20,
					56
			);

			if (clickableArea.contains(x, y)) {
				return i;
			}
		}

		return -1;
	}

	private Rectangle getCellBounds(int index) {
		int col = index % COLS;
		int row = index / COLS;

		int cellX = GRID_X + col * CELL_W;
		int cellY = GRID_Y + row * CELL_H;

		return new Rectangle(cellX, cellY, CELL_W, CELL_H);
	}

	private boolean isBackButtonAt(int x, int y) {
		return x >= BACK_X
				&& x <= BACK_X + BACK_W
				&& y >= BACK_Y
				&& y <= BACK_Y + BACK_H;
	}

	private void backToLevelSelect() {
		context.changeState(new LevelSelectState(context));
	}

	@Override
	public void exit() {
		context.saveSettings();
	}
}