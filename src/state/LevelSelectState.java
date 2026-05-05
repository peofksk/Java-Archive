package state;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import stage.Difficulty;
import state.gameplay.GamePlayState;

public class LevelSelectState implements GameState {

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

	private boolean buttonsAdded = false;

	private final List<JButton> buttons = new ArrayList<>();

	private JButton leftArrowButton;
	private JButton rightArrowButton;
	private JButton titleButton;

	private JButton difficultyPrevButton;
	private JButton difficultyNextButton;
	private JButton difficultyTextButton;

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

		mode = Mode.LEVEL_SELECT;
		context.setCurrentDifficulty(Difficulty.Easy);

		playSample();
		addButtons();
		updateButtonVisibility();
	}

	@Override
	public void update(double deltaTime) {
	}

	@Override
	public void render(Graphics2D g) {
		g.drawImage(background, 0, 0, null);

		Image titleImage = am.getImage(context.sm.getCurrentStage().getTitleImageKey());
		g.drawImage(titleImage, 312, 80, null);

		if (mode == Mode.LEVEL_SELECT) {
			g.drawImage(pressEnter, 357, 500, null);

			if (context.sm.hasPrev()) {
				g.drawImage(arrowLeft, 77, 225, null);
			}

			if (context.sm.hasNext()) {
				g.drawImage(arrowRight, 713, 225, null);
			}
		}

		if (mode == Mode.DIFFICULTY_SELECT) {
			drawDifficultySelect(g);
		}
	}

	private void drawDifficultySelect(Graphics2D g) {
		String label = "Please set difficulty: ";
		String difficultyText = context.getCurrentDifficulty().name();

		int baselineY = 530;

		int labelX = 235;              // 왼쪽으로 더 당김
		int difficultyCenterX = 700;   // 화살표 사이의 중심축
		int arrowOffset = 105;         // 중심축으로부터 화살표 거리

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

		java.awt.FontMetrics difficultyMetrics = g.getFontMetrics();
		int difficultyWidth = difficultyMetrics.stringWidth(difficultyText);
		int difficultyX = difficultyCenterX - difficultyWidth / 2;
		g.drawString(difficultyText, difficultyX, baselineY);

		g.setFont(arrowFont);
		g.setColor(Color.WHITE);

		java.awt.FontMetrics arrowMetrics = g.getFontMetrics();

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

	private void addButtons() {
		if (buttonsAdded) {
			return;
		}

		JPanel panel = context.getGamePanel();
		if (panel == null) {
			return;
		}

		panel.setLayout(null);

		leftArrowButton = createTransparentButton(55, 195, 190, 170);
		rightArrowButton = createTransparentButton(695, 195, 190, 170);
		titleButton = createTransparentButton(295, 65, 430, 260);

		difficultyPrevButton = createTransparentButton(575, 490, 55, 50);
		difficultyNextButton = createTransparentButton(770, 490, 55, 50);
		difficultyTextButton = createTransparentButton(625, 490, 150, 50);

		leftArrowButton.addActionListener(e -> {
			if (mode != Mode.LEVEL_SELECT) {
				return;
			}

			if (context.sm.hasPrev()) {
				context.sm.prev();
				playSample();
				updateButtonVisibility();
				panel.repaint();
			}
		});

		rightArrowButton.addActionListener(e -> {
			if (mode != Mode.LEVEL_SELECT) {
				return;
			}

			if (context.sm.hasNext()) {
				context.sm.next();
				playSample();
				updateButtonVisibility();
				panel.repaint();
			}
		});

		titleButton.addActionListener(e -> {
			if (mode == Mode.LEVEL_SELECT) {
				mode = Mode.DIFFICULTY_SELECT;
				updateButtonVisibility();
				panel.repaint();
				return;
			}

			if (mode == Mode.DIFFICULTY_SELECT) {
				startGame();
			}
		});

		difficultyPrevButton.addActionListener(e -> {
			if (mode != Mode.DIFFICULTY_SELECT) {
				return;
			}

			if (hasLowerDifficulty()) {
				context.setCurrentDifficulty(context.getCurrentDifficulty().prev());
				updateButtonVisibility();
				panel.repaint();
			}
		});

		difficultyNextButton.addActionListener(e -> {
			if (mode != Mode.DIFFICULTY_SELECT) {
				return;
			}

			if (hasHigherDifficulty()) {
				context.setCurrentDifficulty(context.getCurrentDifficulty().next());
				updateButtonVisibility();
				panel.repaint();
			}
		});

		difficultyTextButton.addActionListener(e -> {
			if (mode == Mode.DIFFICULTY_SELECT) {
				startGame();
			}
		});

		addButton(panel, leftArrowButton);
		addButton(panel, rightArrowButton);
		addButton(panel, titleButton);
		addButton(panel, difficultyPrevButton);
		addButton(panel, difficultyNextButton);
		addButton(panel, difficultyTextButton);

		buttonsAdded = true;

		panel.revalidate();
		panel.repaint();
	}

	private JButton createTransparentButton(int x, int y, int width, int height) {
		JButton button = new JButton();

		button.setBounds(new Rectangle(x, y, width, height));
		button.setHorizontalAlignment(SwingConstants.CENTER);

		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setFocusable(false);

		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		return button;
	}

	private void addButton(JPanel panel, JButton button) {
		buttons.add(button);
		panel.add(button);
	}

	private void updateButtonVisibility() {
		if (!buttonsAdded) {
			return;
		}

		boolean levelSelectMode = mode == Mode.LEVEL_SELECT;
		boolean difficultySelectMode = mode == Mode.DIFFICULTY_SELECT;

		if (leftArrowButton != null) {
			leftArrowButton.setVisible(levelSelectMode && context.sm.hasPrev());
			leftArrowButton.setEnabled(levelSelectMode && context.sm.hasPrev());
		}

		if (rightArrowButton != null) {
			rightArrowButton.setVisible(levelSelectMode && context.sm.hasNext());
			rightArrowButton.setEnabled(levelSelectMode && context.sm.hasNext());
		}

		if (titleButton != null) {
			titleButton.setVisible(true);
			titleButton.setEnabled(true);
		}

		if (difficultyPrevButton != null) {
			difficultyPrevButton.setVisible(difficultySelectMode && hasLowerDifficulty());
			difficultyPrevButton.setEnabled(difficultySelectMode && hasLowerDifficulty());
		}

		if (difficultyNextButton != null) {
			difficultyNextButton.setVisible(difficultySelectMode && hasHigherDifficulty());
			difficultyNextButton.setEnabled(difficultySelectMode && hasHigherDifficulty());
		}

		if (difficultyTextButton != null) {
			difficultyTextButton.setVisible(difficultySelectMode);
			difficultyTextButton.setEnabled(difficultySelectMode);
		}
	}

	private boolean hasHigherDifficulty() {
		Difficulty[] difficulties = Difficulty.values();
		return context.getCurrentDifficulty().ordinal() < difficulties.length - 1;
	}

	private boolean hasLowerDifficulty() {
		return context.getCurrentDifficulty().ordinal() > 0;
	}

	private void startGame() {
		GamePlayState next = new GamePlayState(
				context,
				context.sm.getCurrentStage(),
				context.getCurrentDifficulty()
		);

		context.changeState(new LoadState(context, "Loading Game...", () -> next.preload(), () -> next));
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (mode == Mode.LEVEL_SELECT) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				context.changeState(new IntroState(context));
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_C) {
				CalibrationState next = new CalibrationState(context, context.sm.getCurrentCorrectionConfig());
				context.changeState(new LoadState(context, "Loading Calibration...", () -> next.preload(), () -> next));
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_O) {
				context.changeState(new OptionState(context));
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_S) {
				context.changeState(new ShopState(context));
			}

			if (e.getKeyCode() == KeyEvent.VK_LEFT && context.sm.hasPrev()) {
				context.sm.prev();
				playSample();
				updateButtonVisibility();
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_RIGHT && context.sm.hasNext()) {
				context.sm.next();
				playSample();
				updateButtonVisibility();
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				mode = Mode.DIFFICULTY_SELECT;
				updateButtonVisibility();
				return;
			}
		}

		if (mode == Mode.DIFFICULTY_SELECT) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				mode = Mode.LEVEL_SELECT;
				context.setCurrentDifficulty(Difficulty.Easy);
				updateButtonVisibility();
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				if (hasHigherDifficulty()) {
					context.setCurrentDifficulty(context.getCurrentDifficulty().next());
					updateButtonVisibility();
				}
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				if (hasLowerDifficulty()) {
					context.setCurrentDifficulty(context.getCurrentDifficulty().prev());
					updateButtonVisibility();
				}
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				startGame();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void exit() {
		removeButtons();
	}

	private void removeButtons() {
		JPanel panel = context.getGamePanel();
		if (panel == null) {
			return;
		}

		for (JButton button : buttons) {
			panel.remove(button);
		}

		buttons.clear();

		leftArrowButton = null;
		rightArrowButton = null;
		titleButton = null;
		difficultyPrevButton = null;
		difficultyNextButton = null;
		difficultyTextButton = null;

		buttonsAdded = false;

		panel.revalidate();
		panel.repaint();
	}
}