package state;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import state.gameplay.KeyMode;
import state.gameplay.Lane;

public class OptionState implements GameState {

	private final GameContext context;
	private final AssetManager am = AssetManager.getInstance();

	private Image background;

	private boolean preloaded = false;
	private boolean buttonsAdded = false;
	private boolean savedNoticeVisible = false;

	private final List<JButton> buttons = new ArrayList<>();
	private final List<JButton> laneKeyButtons = new ArrayList<>();

	private JButton keyMode4Button;
	private JButton keyMode6Button;
	private JButton backButton;
	private JButton saveButton;

	private Lane waitingKeyLane = null;

	private int pendingKeyCount;
	private final Map<Lane, Integer> pendingKeyBindings = new LinkedHashMap<>();

	public OptionState(GameContext context) {
		this.context = context;
	}

	private void preload() {
		if (preloaded) {
			return;
		}

		context.bgm.load("/optionMusic.wav");
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

		context.bgm.playLoaded(true);
		addButtons();
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

		drawTitle(g);
		drawGuide(g);
		drawSavedNotice(g);
	}

	private void drawTitle(Graphics2D g) {
		g.setColor(new Color(255, 255, 255, 230));
		g.setFont(new Font("Dialog", Font.BOLD, 42));
		g.drawString("OPTIONS", 80, 80);

		g.setFont(new Font("Dialog", Font.BOLD, 24));
		g.drawString("Key Settings", 84, 125);

		g.setFont(new Font("Dialog", Font.BOLD, 18));
		g.drawString("Current Mode: " + pendingKeyCount + "K", 475, 125);
	}

	private void drawGuide(Graphics2D g) {
		g.setColor(new Color(255, 255, 255, 190));
		g.setFont(new Font("Dialog", Font.PLAIN, 15));

		g.drawString("Press 4 / 6 or click buttons to change key mode.", 475, 145);

		if (waitingKeyLane != null) {
			g.setColor(new Color(255, 230, 130));
			g.setFont(new Font("Dialog", Font.BOLD, 17));
			g.drawString("Press a key for: " + waitingKeyLane.getDisplayName(), 475, 420);
		} else {
			g.drawString("Click a lane button, then press a key to rebind.", 475, 420);
		}
	}

	private void drawSavedNotice(Graphics2D g) {
		if (!savedNoticeVisible) {
			return;
		}

		g.setColor(new Color(160, 255, 180, 230));
		g.setFont(new Font("Dialog", Font.BOLD, 16));
		g.drawString("Saved!", 855, 482);
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

		keyMode4Button = createButton("4K Mode", 475, 160, 175, 42);
		keyMode6Button = createButton("6K Mode", 665, 160, 175, 42);

		backButton = createButton("Back", 475, 455, 175, 42);
		saveButton = createButton("Save", 665, 455, 175, 42);

		keyMode4Button.addActionListener(e -> {
			setPendingKeyMode(4);
			panel.repaint();
		});

		keyMode6Button.addActionListener(e -> {
			setPendingKeyMode(6);
			panel.repaint();
		});

		backButton.addActionListener(e -> {
			context.changeState(new LevelSelectState(context));
		});

		saveButton.addActionListener(e -> {
			applyPendingSettingsToContext();
			context.saveSettings();
			savedNoticeVisible = true;
			panel.repaint();
		});

		addButton(panel, keyMode4Button);
		addButton(panel, keyMode6Button);
		addButton(panel, backButton);
		addButton(panel, saveButton);

		rebuildLaneKeyButtons();

		buttonsAdded = true;
		panel.revalidate();
		panel.repaint();
	}

	private JButton createButton(String text, int x, int y, int width, int height) {
		JButton button = new JButton(text);
		button.setBounds(new Rectangle(x, y, width, height));
		button.setFocusPainted(false);
		button.setFocusable(false);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setFont(new Font("Dialog", Font.BOLD, 15));
		button.setBackground(new Color(25, 25, 35));
		button.setForeground(Color.WHITE);
		button.setBorderPainted(false);

		return button;
	}

	private void addButton(JPanel panel, JButton button) {
		buttons.add(button);
		panel.add(button);
	}

	private void setPendingKeyMode(int keyCount) {
		if (pendingKeyCount == keyCount) {
			return;
		}

		pendingKeyCount = keyCount;
		waitingKeyLane = null;
		savedNoticeVisible = false;

		ensurePendingBindingsForCurrentMode();
		rebuildLaneKeyButtons();
	}

	private void ensurePendingBindingsForCurrentMode() {
		for (Lane lane : getPendingPlayableLanes()) {
			pendingKeyBindings.putIfAbsent(lane, lane.getDefaultKeyCode());
		}
	}

	private void rebuildLaneKeyButtons() {
		JPanel panel = context.getGamePanel();
		if (panel == null) {
			return;
		}

		for (JButton button : laneKeyButtons) {
			panel.remove(button);
			buttons.remove(button);
		}

		laneKeyButtons.clear();
		waitingKeyLane = null;

		int leftX = 475;
		int rightX = 665;
		int startY = 230;

		int width = 175;
		int height = 38;
		int gapY = 12;

		List<Lane> playableLanes = getPendingPlayableLanes();
		int leftColumnCount = (int) Math.ceil(playableLanes.size() / 2.0);

		for (int i = 0; i < playableLanes.size(); i++) {
			Lane lane = playableLanes.get(i);

			int column;
			int row;

			if (i < leftColumnCount) {
				column = 0;
				row = i;
			} else {
				column = 1;
				row = i - leftColumnCount;
			}

			int x = column == 0 ? leftX : rightX;
			int y = startY + row * (height + gapY);

			JButton button = createButton(getLaneButtonText(lane), x, y, width, height);

			button.addActionListener(e -> {
				waitingKeyLane = lane;
				savedNoticeVisible = false;
				updateLaneKeyButtonTexts();
				panel.repaint();
			});

			laneKeyButtons.add(button);
			addButton(panel, button);
		}

		updateKeyModeButtonTexts();

		panel.revalidate();
		panel.repaint();
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

	private void updateLaneKeyButtonTexts() {
		List<Lane> playableLanes = getPendingPlayableLanes();

		for (int i = 0; i < laneKeyButtons.size() && i < playableLanes.size(); i++) {
			Lane lane = playableLanes.get(i);
			laneKeyButtons.get(i).setText(getLaneButtonText(lane));
		}

		updateKeyModeButtonTexts();
	}

	private void updateKeyModeButtonTexts() {
		if (keyMode4Button != null) {
			keyMode4Button.setText(pendingKeyCount == 4 ? "4K Mode ✓" : "4K Mode");
		}

		if (keyMode6Button != null) {
			keyMode6Button.setText(pendingKeyCount == 6 ? "6K Mode ✓" : "6K Mode");
		}
	}

	private void applyPendingSettingsToContext() {
		context.setKeyModeByKeyCount(pendingKeyCount);

		for (Lane lane : getPendingPlayableLanes()) {
			int keyCode = getPendingKeyCodeForLane(lane);
			context.setLaneKeyBinding(lane, keyCode);
		}
	}

	@Override
	public void exit() {
		removeButtons();
		waitingKeyLane = null;
		savedNoticeVisible = false;
		pendingKeyBindings.clear();
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
		laneKeyButtons.clear();
		buttonsAdded = false;

		panel.revalidate();
		panel.repaint();
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
			context.changeState(new LevelSelectState(context));
		} else if (e.getKeyCode() == KeyEvent.VK_S) {
			applyPendingSettingsToContext();
			context.saveSettings();
			savedNoticeVisible = true;

			JPanel panel = context.getGamePanel();
			if (panel != null) {
				panel.repaint();
			}
		}
	}

	private void handleKeyBindingInput(KeyEvent e) {
		int keyCode = e.getKeyCode();

		if (keyCode == KeyEvent.VK_ESCAPE) {
			waitingKeyLane = null;
			updateLaneKeyButtonTexts();

			JPanel panel = context.getGamePanel();
			if (panel != null) {
				panel.repaint();
			}

			return;
		}

		if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_UNDEFINED) {
			return;
		}

		removeDuplicatePendingBinding(keyCode);
		pendingKeyBindings.put(waitingKeyLane, keyCode);

		waitingKeyLane = null;
		savedNoticeVisible = false;
		updateLaneKeyButtonTexts();

		JPanel panel = context.getGamePanel();
		if (panel != null) {
			panel.repaint();
		}
	}

	private void removeDuplicatePendingBinding(int keyCode) {
		for (Lane lane : getPendingPlayableLanes()) {
			if (lane == waitingKeyLane) {
				continue;
			}

			int existingKeyCode = getPendingKeyCodeForLane(lane);

			if (existingKeyCode == keyCode) {
				pendingKeyBindings.put(lane, lane.getDefaultKeyCode());
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
}