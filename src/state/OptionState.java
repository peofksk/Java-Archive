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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import state.gameplay.KeyMode;
import state.gameplay.Lane;

public class OptionState implements GameState {

	private static final int BUTTON_KEY_MODE_4 = 0;
	private static final int BUTTON_KEY_MODE_6 = 1;
	private static final int BUTTON_SAVE = 2;

	private static final int LEFT_X = 475;
	private static final int RIGHT_X = 665;

	private static final int MODE_TEXT_Y = 128;
	private static final int GUIDE_TOP_Y = 148;

	private static final int MODE_Y = 176;
	private static final int MODE_BUTTON_W = 175;
	private static final int MODE_BUTTON_H = 42;

	private static final int LANE_START_Y = 246;
	private static final int LANE_BUTTON_W = 175;
	private static final int LANE_BUTTON_H = 38;
	private static final int LANE_GAP_Y = 12;

	private static final int WARNING_Y = 405;
	private static final int GUIDE_BOTTOM_Y = 430;

	private static final int BOTTOM_Y = 468;
	private static final int BOTTOM_BUTTON_W = 175;
	private static final int BOTTOM_BUTTON_H = 42;

	private static final int BACK_BUTTON_W = 96;
	private static final int BACK_BUTTON_H = 36;
	private static final int BACK_BUTTON_X = 808;
	private static final int BACK_BUTTON_Y = 94;

	private final GameContext context;
	private final AssetManager am = AssetManager.getInstance();

	private Image background;

	private boolean preloaded = false;
	private boolean savedNoticeVisible = false;

	private Lane waitingKeyLane = null;
	private String warningMessage = null;

	private int pendingKeyCount;
	private final Map<Lane, Integer> pendingKeyBindings = new LinkedHashMap<>();

	private int hoveredFixedButton = -1;
	private int pressedFixedButton = -1;

	private Lane hoveredLaneButton = null;
	private Lane pressedLaneButton = null;

	private boolean backButtonHovered = false;
	private boolean backButtonPressed = false;

	public OptionState(GameContext context) {
		this.context = context;
	}

	private void preload() {
		if (preloaded) {
			return;
		}

		context.bgm.load("/audio/bgm/optionMusic.wav");
		background = am.getImage("option_bg");

		preloaded = true;
	}

	@Override
	public void enter() {
		context.bgm.stop();

		if (!preloaded) {
			preload();
		}

		copySettingsFromContext();

		waitingKeyLane = null;
		warningMessage = null;
		savedNoticeVisible = false;

		hoveredFixedButton = -1;
		pressedFixedButton = -1;
		hoveredLaneButton = null;
		pressedLaneButton = null;

		backButtonHovered = false;
		backButtonPressed = false;

		context.bgm.playLoaded(true);
	}

	private void copySettingsFromContext() {
		pendingKeyCount = context.getKeyCount();
		pendingKeyBindings.clear();

		for (KeyMode mode : KeyMode.values()) {
			for (Lane lane : mode.getLanes()) {
				int keyCode = context.getKeyCodeForLane(lane);

				if (keyCode == KeyEvent.VK_UNDEFINED) {
					keyCode = lane.getDefaultKeyCode();
				}

				pendingKeyBindings.put(lane, keyCode);
			}
		}

		ensurePendingBindingsForCurrentMode();
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

		Object oldAntialiasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		drawGuide(g);
		drawButtons(g);
		drawSavedNotice(g);
		drawBackButton(g);

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
	}

	private void drawGuide(Graphics2D g) {
		g.setColor(new Color(255, 255, 255, 230));
		g.setFont(new Font("Dialog", Font.BOLD, 18));
		g.drawString("Current Mode: " + pendingKeyCount + "K", 475, MODE_TEXT_Y);

		g.setColor(new Color(255, 255, 255, 190));
		g.setFont(new Font("Dialog", Font.PLAIN, 15));
		g.drawString("Press 4 / 6 or click buttons to change key mode.", 475, GUIDE_TOP_Y);

		if (warningMessage != null && !warningMessage.isBlank()) {
			g.setColor(new Color(255, 220, 90, 235));
			g.setFont(new Font("Dialog", Font.BOLD, 15));
			g.drawString(warningMessage, 475, WARNING_Y);
		}

		if (waitingKeyLane != null) {
			g.setColor(new Color(255, 230, 130));
			g.setFont(new Font("Dialog", Font.BOLD, 17));
			g.drawString("Press a key for: " + waitingKeyLane.getDisplayName(), 475, GUIDE_BOTTOM_Y);
		} else {
			g.setColor(new Color(255, 255, 255, 190));
			g.setFont(new Font("Dialog", Font.PLAIN, 15));
			g.drawString("Click a lane button, then press a key to rebind.", 475, GUIDE_BOTTOM_Y);
		}
	}

	private void drawButtons(Graphics2D g) {
		drawFixedButton(g, BUTTON_KEY_MODE_4, getFixedButtonText(BUTTON_KEY_MODE_4));
		drawFixedButton(g, BUTTON_KEY_MODE_6, getFixedButtonText(BUTTON_KEY_MODE_6));

		for (Lane lane : getPendingPlayableLanes()) {
			drawLaneButton(g, lane);
		}

		drawFixedButton(g, BUTTON_SAVE, "Save");
	}

	private String getFixedButtonText(int buttonIndex) {
		if (buttonIndex == BUTTON_KEY_MODE_4) {
			return pendingKeyCount == 4 ? "4K Mode ✓" : "4K Mode";
		}

		if (buttonIndex == BUTTON_KEY_MODE_6) {
			return pendingKeyCount == 6 ? "6K Mode ✓" : "6K Mode";
		}

		return "";
	}

	private void drawFixedButton(Graphics2D g, int buttonIndex, String text) {
		Rectangle bounds = getFixedButtonBounds(buttonIndex);

		boolean hovered = hoveredFixedButton == buttonIndex;
		boolean pressed = pressedFixedButton == buttonIndex;
		boolean selected = (buttonIndex == BUTTON_KEY_MODE_4 && pendingKeyCount == 4)
				|| (buttonIndex == BUTTON_KEY_MODE_6 && pendingKeyCount == 6);

		drawButton(g, bounds, text, hovered, pressed, selected);
	}

	private void drawLaneButton(Graphics2D g, Lane lane) {
		Rectangle bounds = getLaneButtonBounds(lane);

		boolean hovered = hoveredLaneButton == lane;
		boolean pressed = pressedLaneButton == lane;
		boolean selected = waitingKeyLane == lane;

		drawButton(g, bounds, getLaneButtonText(lane), hovered, pressed, selected);
	}

	private void drawButton(
			Graphics2D g,
			Rectangle bounds,
			String text,
			boolean hovered,
			boolean pressed,
			boolean selected
	) {
		Color fillColor;
		Color borderColor;
		Color textColor;

		if (pressed) {
			fillColor = new Color(35, 105, 165, 220);
			borderColor = new Color(210, 245, 255, 255);
			textColor = Color.WHITE;
		} else if (selected) {
			fillColor = new Color(45, 135, 95, 210);
			borderColor = new Color(180, 255, 210, 245);
			textColor = Color.WHITE;
		} else if (hovered) {
			fillColor = new Color(60, 120, 180, 200);
			borderColor = new Color(190, 235, 255, 245);
			textColor = Color.WHITE;
		} else {
			fillColor = new Color(25, 25, 35, 210);
			borderColor = new Color(120, 170, 210, 160);
			textColor = new Color(235, 248, 255);
		}

		g.setColor(new Color(0, 0, 0, 120));
		g.fillRoundRect(bounds.x + 3, bounds.y + 4, bounds.width, bounds.height, 14, 14);

		g.setColor(fillColor);
		g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);

		g.setStroke(new BasicStroke(2f));
		g.setColor(borderColor);
		g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);

		g.setFont(new Font("Dialog", Font.BOLD, 15));
		drawCenteredString(g, text, bounds, textColor);
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

	private void drawCenteredString(Graphics2D g, String text, Rectangle bounds, Color color) {
		FontMetrics fm = g.getFontMetrics();

		int textX = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
		int textY = bounds.y + ((bounds.height - fm.getHeight()) / 2) + fm.getAscent();

		g.setColor(new Color(0, 0, 0, 130));
		g.drawString(text, textX + 1, textY + 1);

		g.setColor(color);
		g.drawString(text, textX, textY);
	}

	private void drawSavedNotice(Graphics2D g) {
		if (!savedNoticeVisible) {
			return;
		}

		g.setColor(new Color(160, 255, 180, 230));
		g.setFont(new Font("Dialog", Font.BOLD, 16));
		g.drawString("Saved!", 855, 493);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (waitingKeyLane != null) {
			handleKeyBindingInput(e);
			return;
		}

		if (e.getKeyCode() == KeyEvent.VK_4) {
			setPendingKeyMode(4);
		} else if (e.getKeyCode() == KeyEvent.VK_6) {
			setPendingKeyMode(6);
		} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			backToLevelSelect();
		} else if (e.getKeyCode() == KeyEvent.VK_S) {
			saveSettings();
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

		pressedFixedButton = getFixedButtonIndexAt(point);
		pressedLaneButton = getLaneButtonAt(point);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1) {
			backButtonPressed = false;
			clearPressedState();
			return;
		}

		Point point = e.getPoint();

		if (backButtonPressed && getBackButtonBounds().contains(point)) {
			backButtonPressed = false;
			clearPressedState();
			backToLevelSelect();
			return;
		}

		backButtonPressed = false;

		int releasedFixedButton = getFixedButtonIndexAt(point);
		Lane releasedLaneButton = getLaneButtonAt(point);

		if (pressedFixedButton >= 0 && pressedFixedButton == releasedFixedButton) {
			executeFixedButton(releasedFixedButton);
			clearPressedState();
			return;
		}

		if (pressedLaneButton != null && pressedLaneButton == releasedLaneButton) {
			selectLaneForKeyBinding(releasedLaneButton);
			clearPressedState();
			return;
		}

		clearPressedState();
	}

	private void updateHoverState(Point point) {
		hoveredFixedButton = getFixedButtonIndexAt(point);
		hoveredLaneButton = getLaneButtonAt(point);
	}

	private void clearPressedState() {
		pressedFixedButton = -1;
		pressedLaneButton = null;
	}

	private int getFixedButtonIndexAt(Point point) {
		int[] indexes = {
				BUTTON_KEY_MODE_4,
				BUTTON_KEY_MODE_6,
				BUTTON_SAVE
		};

		for (int index : indexes) {
			if (getFixedButtonBounds(index).contains(point)) {
				return index;
			}
		}

		return -1;
	}

	private Lane getLaneButtonAt(Point point) {
		for (Lane lane : getPendingPlayableLanes()) {
			if (getLaneButtonBounds(lane).contains(point)) {
				return lane;
			}
		}

		return null;
	}

	private Rectangle getFixedButtonBounds(int buttonIndex) {
		return switch (buttonIndex) {
			case BUTTON_KEY_MODE_4 -> new Rectangle(LEFT_X, MODE_Y, MODE_BUTTON_W, MODE_BUTTON_H);
			case BUTTON_KEY_MODE_6 -> new Rectangle(RIGHT_X, MODE_Y, MODE_BUTTON_W, MODE_BUTTON_H);
			case BUTTON_SAVE -> new Rectangle(RIGHT_X, BOTTOM_Y, BOTTOM_BUTTON_W, BOTTOM_BUTTON_H);
			default -> new Rectangle();
		};
	}

	private Rectangle getBackButtonBounds() {
		return new Rectangle(BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_W, BACK_BUTTON_H);
	}

	private Rectangle getLaneButtonBounds(Lane lane) {
		List<Lane> playableLanes = getPendingPlayableLanes();

		int index = playableLanes.indexOf(lane);
		if (index < 0) {
			return new Rectangle();
		}

		int leftColumnCount = (int) Math.ceil(playableLanes.size() / 2.0);

		int column;
		int row;

		if (index < leftColumnCount) {
			column = 0;
			row = index;
		} else {
			column = 1;
			row = index - leftColumnCount;
		}

		int x = column == 0 ? LEFT_X : RIGHT_X;
		int y = LANE_START_Y + row * (LANE_BUTTON_H + LANE_GAP_Y);

		return new Rectangle(x, y, LANE_BUTTON_W, LANE_BUTTON_H);
	}

	private void executeFixedButton(int buttonIndex) {
		switch (buttonIndex) {
			case BUTTON_KEY_MODE_4 -> setPendingKeyMode(4);
			case BUTTON_KEY_MODE_6 -> setPendingKeyMode(6);
			case BUTTON_SAVE -> saveSettings();
		}
	}

	private void selectLaneForKeyBinding(Lane lane) {
		if (lane == null) {
			return;
		}

		waitingKeyLane = lane;
		warningMessage = null;
		savedNoticeVisible = false;
	}

	private void setPendingKeyMode(int keyCount) {
		if (pendingKeyCount == keyCount) {
			return;
		}

		pendingKeyCount = keyCount;
		waitingKeyLane = null;
		warningMessage = null;
		savedNoticeVisible = false;
		hoveredLaneButton = null;
		pressedLaneButton = null;

		ensurePendingBindingsForCurrentMode();
	}

	private void ensurePendingBindingsForCurrentMode() {
		for (Lane lane : getPendingPlayableLanes()) {
			pendingKeyBindings.putIfAbsent(lane, lane.getDefaultKeyCode());
		}
	}

	private List<Lane> getPendingPlayableLanes() {
		List<Lane> lanes = new ArrayList<>();

		for (Lane lane : KeyMode.fromKeyCount(pendingKeyCount).getLanes()) {
			lanes.add(lane);
		}

		return lanes;
	}

	private String getLaneButtonText(Lane lane) {
		String keyText = KeyEvent.getKeyText(getPendingKeyCodeForLane(lane));

		if (waitingKeyLane == lane) {
			return lane.getDisplayName() + " : [Press Key]";
		}

		return lane.getDisplayName() + " : " + keyText;
	}

	private int getPendingKeyCodeForLane(Lane lane) {
		Integer keyCode = pendingKeyBindings.get(lane);

		if (keyCode == null || keyCode == KeyEvent.VK_UNDEFINED) {
			return lane.getDefaultKeyCode();
		}

		return keyCode;
	}

	private void saveSettings() {
		applyPendingSettingsToContext();
		context.saveSettings();
		savedNoticeVisible = true;
		waitingKeyLane = null;
		warningMessage = null;
	}

	private void applyPendingSettingsToContext() {
		context.setKeyModeByKeyCount(pendingKeyCount);

		for (Lane lane : getPendingPlayableLanes()) {
			int keyCode = getPendingKeyCodeForLane(lane);
			context.setLaneKeyBinding(lane, keyCode);
		}
	}

	private void handleKeyBindingInput(KeyEvent e) {
		int keyCode = e.getKeyCode();

		if (keyCode == KeyEvent.VK_ESCAPE) {
			waitingKeyLane = null;
			warningMessage = null;
			return;
		}

		if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_UNDEFINED) {
			return;
		}

		Lane duplicatedLane = findLaneUsingKeyCode(keyCode);

		if (duplicatedLane != null && duplicatedLane != waitingKeyLane) {
			String keyText = KeyEvent.getKeyText(keyCode);

			warningMessage = keyText + " is already assigned to " + duplicatedLane.getDisplayName() + ". Change canceled.";
			waitingKeyLane = null;
			savedNoticeVisible = false;
			return;
		}

		pendingKeyBindings.put(waitingKeyLane, keyCode);

		waitingKeyLane = null;
		warningMessage = null;
		savedNoticeVisible = false;
	}

	private Lane findLaneUsingKeyCode(int keyCode) {
		for (Lane lane : getPendingPlayableLanes()) {
			if (lane == waitingKeyLane) {
				continue;
			}

			int existingKeyCode = getPendingKeyCodeForLane(lane);

			if (existingKeyCode == keyCode) {
				return lane;
			}
		}

		return null;
	}

	private void backToLevelSelect() {
		context.changeState(new LevelSelectState(context));
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void exit() {
		waitingKeyLane = null;
		warningMessage = null;
		savedNoticeVisible = false;
		pendingKeyBindings.clear();

		hoveredFixedButton = -1;
		pressedFixedButton = -1;
		hoveredLaneButton = null;
		pressedLaneButton = null;

		backButtonHovered = false;
		backButtonPressed = false;
	}
}