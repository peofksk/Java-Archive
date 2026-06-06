package state;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import stage.Difficulty;
import state.gameplay.GamePlayState;

public class LevelSelectState implements GameState {
	private static final int BUTTON_START = 0;
	private static final int BUTTON_OPTION = 1;
	private static final int BUTTON_CALIBRATION = 2;
	private static final int BUTTON_SHOP = 3;

	private static final int MENU_X = 745;
	private static final int MENU_Y = 365;
	private static final int MENU_W = 180;
	private static final int MENU_H = 34;
	private static final int MENU_GAP = 8;

	private static final int BACK_BUTTON_W = 96;
	private static final int BACK_BUTTON_H = 36;
	private static final int BACK_BUTTON_X = 1024 - BACK_BUTTON_W - 36;
	private static final int BACK_BUTTON_Y = 34;

	private final GameContext context;
	private final AssetManager am = AssetManager.getInstance();

	private Image background;
	private Image arrowLeft;
	private Image arrowRight;
	private Image pressEnter;

	private enum Mode {
		LEVEL_SELECT,
		DIFFICULTY_SELECT
	}

	private Mode mode = Mode.LEVEL_SELECT;

	private int hoveredMenuButton = -1;
	private int pressedMenuButton = -1;

	private boolean backButtonHovered = false;
	private boolean backButtonPressed = false;

	private boolean leftArrowHovered = false;
	private boolean rightArrowHovered = false;
	private boolean titleHovered = false;

	private boolean difficultyPrevHovered = false;
	private boolean difficultyNextHovered = false;
	private boolean difficultyTextHovered = false;

	public LevelSelectState(GameContext context) {
		this.context = context;
	}

	private void playSample() {
		context.bgm.play(context.sm.getCurrentStage().getSamplePath(), true);
	}

	@Override
	public void enter() {
		background = am.getImage("level_selection_bg");
		arrowLeft = am.getImage("arrow_left");
		arrowRight = am.getImage("arrow_right");
		pressEnter = am.getImage("press_enter");

		mode = Mode.LEVEL_SELECT;
		context.setCurrentDifficulty(Difficulty.Easy);

		hoveredMenuButton = -1;
		pressedMenuButton = -1;
		backButtonHovered = false;
		backButtonPressed = false;

		playSample();
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

		Image titleImage = am.getImage(context.sm.getCurrentStage().getTitleImageKey());
		if (titleImage != null) {
			g.drawImage(titleImage, 312, 80, null);
		}

		if (mode == Mode.LEVEL_SELECT) {
			if (pressEnter != null) {
				g.drawImage(pressEnter, 357, 500, null);
			}

			if (context.sm.hasPrev() && arrowLeft != null) {
				g.drawImage(arrowLeft, 77, 225, null);
			}

			if (context.sm.hasNext() && arrowRight != null) {
				g.drawImage(arrowRight, 713, 225, null);
			}

			drawMenuButtons(g);
		}

		if (mode == Mode.DIFFICULTY_SELECT) {
			drawDifficultySelect(g);
		}

		drawBackButton(g);
		drawAutoStatus(g);
	}

	private void drawMenuButtons(Graphics2D g) {
		Object oldAntialiasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		drawMenuButton(g, BUTTON_START, "START");
		drawMenuButton(g, BUTTON_OPTION, "OPTION");
		drawMenuButton(g, BUTTON_CALIBRATION, "CALIBRATION");
		drawMenuButton(g, BUTTON_SHOP, "SHOP");

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
	}

	private void drawMenuButton(Graphics2D g, int index, String text) {
		Rectangle bounds = getMenuButtonBounds(index);

		boolean hovered = hoveredMenuButton == index;
		boolean pressed = pressedMenuButton == index;

		Color fillColor;
		Color borderColor;
		Color textColor;

		if (pressed) {
			fillColor = new Color(35, 105, 165, 210);
			borderColor = new Color(205, 240, 255, 250);
			textColor = new Color(255, 255, 255, 255);
		} else if (hovered) {
			fillColor = new Color(70, 145, 205, 190);
			borderColor = new Color(190, 235, 255, 245);
			textColor = new Color(255, 255, 255, 255);
		} else {
			fillColor = new Color(0, 0, 0, 95);
			borderColor = new Color(120, 190, 245, 180);
			textColor = new Color(230, 245, 255, 230);
		}

		g.setColor(new Color(0, 0, 0, 100));
		g.fillRoundRect(bounds.x + 3, bounds.y + 4, bounds.width, bounds.height, 18, 18);

		g.setColor(fillColor);
		g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 18, 18);

		g.setStroke(new BasicStroke(2f));
		g.setColor(borderColor);
		g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 18, 18);

		g.setFont(new Font("SansSerif", Font.BOLD, 15));
		drawCenteredString(g, text, bounds, textColor);
	}

	private void drawDifficultySelect(Graphics2D g) {
		String label = "Please set difficulty: ";
		String difficultyText = context.getCurrentDifficulty().name();

		int baselineY = 530;
		int labelX = 235;
		int difficultyCenterX = 700;
		int arrowOffset = 105;

		Font labelFont = new Font("SansSerif", Font.ITALIC, 35);
		Font difficultyFont = new Font("SansSerif", Font.BOLD, 35);
		Font arrowFont = new Font("SansSerif", Font.BOLD, 34);

		g.setFont(labelFont);
		g.setColor(Color.CYAN);
		g.drawString(label, labelX, baselineY);

		g.setFont(difficultyFont);
		if (context.getCurrentDifficulty() == Difficulty.Easy) {
			g.setColor(Color.GREEN);
		} else if (context.getCurrentDifficulty() == Difficulty.Hard) {
			g.setColor(Color.ORANGE);
		} else if (context.getCurrentDifficulty() == Difficulty.Extreme) {
			g.setColor(Color.RED);
		}

		FontMetrics difficultyMetrics = g.getFontMetrics();
		int difficultyWidth = difficultyMetrics.stringWidth(difficultyText);
		int difficultyX = difficultyCenterX - difficultyWidth / 2;
		g.drawString(difficultyText, difficultyX, baselineY);

		g.setFont(arrowFont);
		g.setColor(Color.WHITE);

		FontMetrics arrowMetrics = g.getFontMetrics();

		if (hasLowerDifficulty()) {
			String leftArrow = "◀";
			int leftArrowCenterX = difficultyCenterX - arrowOffset;
			int leftArrowX = leftArrowCenterX - arrowMetrics.stringWidth(leftArrow) / 2;
			g.drawString(leftArrow, leftArrowX, baselineY);
		}

		if (hasHigherDifficulty()) {
			String rightArrow = "▶";
			int rightArrowCenterX = difficultyCenterX + arrowOffset;
			int rightArrowX = rightArrowCenterX - arrowMetrics.stringWidth(rightArrow) / 2;
			g.drawString(rightArrow, rightArrowX, baselineY);
		}
	}

	private void drawBackButton(Graphics2D g) {
		Graphics2D g2 = (Graphics2D) g.create();

		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Rectangle bounds = getBackButtonBounds();

			Color fill;
			Color border;
			Color text;

			if (backButtonPressed) {
				fill = new Color(35, 105, 165, 230);
				border = new Color(210, 245, 255, 255);
				text = Color.WHITE;
			} else if (backButtonHovered) {
				fill = new Color(70, 145, 205, 210);
				border = new Color(190, 235, 255, 245);
				text = Color.WHITE;
			} else {
				fill = new Color(0, 0, 0, 135);
				border = new Color(130, 190, 230, 210);
				text = new Color(235, 248, 255);
			}

			g2.setColor(new Color(0, 0, 0, 120));
			g2.fillRoundRect(bounds.x + 3, bounds.y + 4, bounds.width, bounds.height, 14, 14);

			g2.setColor(fill);
			g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);

			g2.setStroke(new BasicStroke(2f));
			g2.setColor(border);
			g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);

			g2.setFont(new Font("Arial", Font.BOLD, 16));
			drawCenteredString(g2, "BACK", bounds, text);
		} finally {
			g2.dispose();
		}
	}

	private void drawAutoStatus(Graphics2D g) {
		if (!context.isAutoMode()) {
			return;
		}

		String text = "AUTO ON";

		g.setFont(new Font("SansSerif", Font.BOLD, 18));
		g.setColor(new Color(255, 120, 255));

		int x = 20;
		int y = 32;

		g.drawString(text, x, y);
	}

	private void drawCenteredString(Graphics2D g, String text, Rectangle bounds, Color color) {
		FontMetrics fm = g.getFontMetrics();
		int textX = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
		int textY = bounds.y + ((bounds.height - fm.getHeight()) / 2) + fm.getAscent();

		g.setColor(new Color(0, 0, 0, 130));
		g.drawString(text, textX + 1, textY + 1);

		g.setColor(color);
		g.drawString(text, textX, textY);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (mode == Mode.LEVEL_SELECT) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				backToIntro();
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_C) {
				openCalibration();
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_O) {
				openOption();
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_S) {
				openShop();
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_LEFT && context.sm.hasPrev()) {
				selectPreviousStage();
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_RIGHT && context.sm.hasNext()) {
				selectNextStage();
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				enterDifficultySelectMode();
				return;
			}
		}

		if (mode == Mode.DIFFICULTY_SELECT) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				mode = Mode.LEVEL_SELECT;
				context.setCurrentDifficulty(Difficulty.Easy);
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				if (hasHigherDifficulty()) {
					context.setCurrentDifficulty(context.getCurrentDifficulty().next());
				}
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				if (hasLowerDifficulty()) {
					context.setCurrentDifficulty(context.getCurrentDifficulty().prev());
				}
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				startGame();
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		backButtonHovered = getBackButtonBounds().contains(e.getPoint());
		updateHoverState(e.getPoint());
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		mouseMoved(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1) {
			return;
		}

		Point point = e.getPoint();

		backButtonPressed = getBackButtonBounds().contains(point);
		if (backButtonPressed) {
			return;
		}

		if (mode == Mode.LEVEL_SELECT) {
			pressedMenuButton = getMenuButtonIndexAt(point);
			if (pressedMenuButton >= 0) {
				return;
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1) {
			backButtonPressed = false;
			pressedMenuButton = -1;
			return;
		}

		Point point = e.getPoint();

		if (backButtonPressed && getBackButtonBounds().contains(point)) {
			backButtonPressed = false;
			pressedMenuButton = -1;
			backToIntro();
			return;
		}

		backButtonPressed = false;

		if (mode == Mode.LEVEL_SELECT) {
			int releasedMenuButton = getMenuButtonIndexAt(point);

			if (pressedMenuButton >= 0 && pressedMenuButton == releasedMenuButton) {
				executeMenuButton(releasedMenuButton);
				pressedMenuButton = -1;
				return;
			}

			if (getLeftArrowBounds().contains(point) && context.sm.hasPrev()) {
				selectPreviousStage();
				pressedMenuButton = -1;
				return;
			}

			if (getRightArrowBounds().contains(point) && context.sm.hasNext()) {
				selectNextStage();
				pressedMenuButton = -1;
				return;
			}

			if (getTitleBounds().contains(point)) {
				enterDifficultySelectMode();
				pressedMenuButton = -1;
				return;
			}
		}

		if (mode == Mode.DIFFICULTY_SELECT) {
			if (getDifficultyPrevBounds().contains(point) && hasLowerDifficulty()) {
				context.setCurrentDifficulty(context.getCurrentDifficulty().prev());
				pressedMenuButton = -1;
				return;
			}

			if (getDifficultyNextBounds().contains(point) && hasHigherDifficulty()) {
				context.setCurrentDifficulty(context.getCurrentDifficulty().next());
				pressedMenuButton = -1;
				return;
			}

			if (getDifficultyTextBounds().contains(point)) {
				startGame();
				pressedMenuButton = -1;
				return;
			}
		}

		pressedMenuButton = -1;
	}

	private void updateHoverState(Point point) {
		if (mode == Mode.LEVEL_SELECT) {
			hoveredMenuButton = getMenuButtonIndexAt(point);
			leftArrowHovered = getLeftArrowBounds().contains(point) && context.sm.hasPrev();
			rightArrowHovered = getRightArrowBounds().contains(point) && context.sm.hasNext();
			titleHovered = getTitleBounds().contains(point);
		} else {
			hoveredMenuButton = -1;
			leftArrowHovered = false;
			rightArrowHovered = false;
			titleHovered = false;
		}

		if (mode == Mode.DIFFICULTY_SELECT) {
			difficultyPrevHovered = getDifficultyPrevBounds().contains(point) && hasLowerDifficulty();
			difficultyNextHovered = getDifficultyNextBounds().contains(point) && hasHigherDifficulty();
			difficultyTextHovered = getDifficultyTextBounds().contains(point);
		} else {
			difficultyPrevHovered = false;
			difficultyNextHovered = false;
			difficultyTextHovered = false;
		}
	}

	private int getMenuButtonIndexAt(Point point) {
		for (int i = 0; i < 4; i++) {
			if (getMenuButtonBounds(i).contains(point)) {
				return i;
			}
		}

		return -1;
	}

	private Rectangle getMenuButtonBounds(int index) {
		int y = MENU_Y + index * (MENU_H + MENU_GAP);
		return new Rectangle(MENU_X, y, MENU_W, MENU_H);
	}

	private Rectangle getBackButtonBounds() {
		return new Rectangle(BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_W, BACK_BUTTON_H);
	}

	private Rectangle getLeftArrowBounds() {
		return new Rectangle(55, 195, 190, 170);
	}

	private Rectangle getRightArrowBounds() {
		return new Rectangle(695, 195, 190, 170);
	}

	private Rectangle getTitleBounds() {
		return new Rectangle(295, 65, 430, 260);
	}

	private Rectangle getDifficultyPrevBounds() {
		return new Rectangle(575, 490, 55, 50);
	}

	private Rectangle getDifficultyNextBounds() {
		return new Rectangle(770, 490, 55, 50);
	}

	private Rectangle getDifficultyTextBounds() {
		return new Rectangle(625, 490, 150, 50);
	}

	private void executeMenuButton(int buttonIndex) {
		switch (buttonIndex) {
			case BUTTON_START -> enterDifficultySelectMode();
			case BUTTON_OPTION -> openOption();
			case BUTTON_CALIBRATION -> openCalibration();
			case BUTTON_SHOP -> openShop();
		}
	}

	private void selectPreviousStage() {
		if (!context.sm.hasPrev()) {
			return;
		}

		context.sm.prev();
		playSample();
	}

	private void selectNextStage() {
		if (!context.sm.hasNext()) {
			return;
		}

		context.sm.next();
		playSample();
	}

	private boolean hasHigherDifficulty() {
		Difficulty[] difficulties = Difficulty.values();
		return context.getCurrentDifficulty().ordinal() < difficulties.length - 1;
	}

	private boolean hasLowerDifficulty() {
		return context.getCurrentDifficulty().ordinal() > 0;
	}

	private void enterDifficultySelectMode() {
		mode = Mode.DIFFICULTY_SELECT;
		hoveredMenuButton = -1;
		pressedMenuButton = -1;
	}

	private void openOption() {
		context.changeState(new OptionState(context));
	}

	private void openCalibration() {
		CalibrationState next = new CalibrationState(context, context.sm.getCurrentCorrectionConfig());
		context.changeState(new LoadState(context, "Loading Calibration...", () -> next.preload(), () -> next));
	}

	private void openShop() {
		context.changeState(new ShopState(context));
	}

	private void startGame() {
		GamePlayState next = new GamePlayState(
				context,
				context.sm.getCurrentStage(),
				context.getCurrentDifficulty()
		);

		context.changeState(new LoadState(context, "Loading Game...", () -> next.preload(), () -> next));
	}

	private void backToIntro() {
		context.changeState(new IntroState(context));
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void exit() {
		hoveredMenuButton = -1;
		pressedMenuButton = -1;
		backButtonHovered = false;
		backButtonPressed = false;
	}
}