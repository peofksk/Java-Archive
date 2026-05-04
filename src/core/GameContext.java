package core;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import audio.BGMManager;
import stage.Difficulty;
import stage.StageManager;
import state.gameplay.KeyMode;
import state.gameplay.Lane;

public class GameContext {
	public BGMManager bgm = new BGMManager();
	public StageManager sm = new StageManager();

	private static final String SETTINGS_DIRECTORY_NAME = ".Java-Archive";
	private static final String SETTINGS_FILE_NAME = "settings.properties";

	private final Path settingsPath = Paths.get(
			System.getProperty("user.home"),
			SETTINGS_DIRECTORY_NAME,
			SETTINGS_FILE_NAME
	);

	private GameState currentState;
	private GamePanel gamePanel;
	private Difficulty currentDifficulty = Difficulty.Easy;
	private double GLOBAL_OFFSET = -0.08;

	private KeyMode keyMode = KeyMode.KEY_4;
	private final Map<Lane, Integer> laneKeyBindings = new LinkedHashMap<>();

	private final int NOTE_COUNT = 12;
	private int noteIndex = 0;

	public GameContext() {
		resetLaneKeyBindingsToDefault();
		loadSettings();
	}

	public void changeState(GameState next) {
		if (currentState != null) {
			currentState.exit();
		}

		currentState = next;

		if (currentState != null) {
			currentState.enter();
		}
	}

	public void setGamePanel(GamePanel gamePanel) {
		this.gamePanel = gamePanel;
	}
	
	public GamePanel getGamePanel() {
		return gamePanel;
	}
	
	public GameState getCurrentState() {
		return currentState;
	}

	public Difficulty getCurrentDifficulty() {
		return currentDifficulty;
	}

	public void setCurrentDifficulty(Difficulty currentDifficulty) {
		if (currentDifficulty == null) {
			return;
		}

		this.currentDifficulty = currentDifficulty;
	}

	public double getGlobalOffset() {
		return GLOBAL_OFFSET;
	}

	public void setGlobalOffset(double newGlobalOffset) {
		this.GLOBAL_OFFSET = newGlobalOffset;
	}

	public int getNoteCount() {
		return NOTE_COUNT;
	}

	public int getNoteIndex() {
		return noteIndex;
	}

	public void setNoteIndex(int noteIndex) {
		if (noteIndex < 0) {
			this.noteIndex = 0;
		} else if (noteIndex >= NOTE_COUNT) {
			this.noteIndex = NOTE_COUNT - 1;
		} else {
			this.noteIndex = noteIndex;
		}
	}

	public void nextNoteIndex() {
		if (noteIndex < NOTE_COUNT - 1) {
			noteIndex++;
		}
	}

	public void prevNoteIndex() {
		if (noteIndex > 0) {
			noteIndex--;
		}
	}

	public KeyMode getKeyMode() {
		return keyMode;
	}

	public int getKeyCount() {
		return keyMode.getKeyCount();
	}

	public void setKeyMode(KeyMode keyMode) {
		if (keyMode == null) {
			return;
		}

		this.keyMode = keyMode;
		ensureLaneKeyBindings();
	}

	public void setKeyModeByKeyCount(int keyCount) {
		setKeyMode(KeyMode.fromKeyCount(keyCount));
	}

	public List<Lane> getPlayableLanes() {
		ArrayList<Lane> lanes = new ArrayList<>();
		Collections.addAll(lanes, keyMode.getLanes());
		return Collections.unmodifiableList(lanes);
	}

	public int getLaneCount() {
		return keyMode.getLanes().length;
	}

	public boolean isPlayableLane(Lane lane) {
		if (lane == null) {
			return false;
		}

		for (Lane playableLane : keyMode.getLanes()) {
			if (playableLane == lane) {
				return true;
			}
		}

		return false;
	}

	public int getLaneIndex(Lane lane) {
		if (lane == null) {
			return -1;
		}

		Lane[] lanes = keyMode.getLanes();
		for (int i = 0; i < lanes.length; i++) {
			if (lanes[i] == lane) {
				return i;
			}
		}

		return -1;
	}

	public void resetLaneKeyBindingsToDefault() {
		laneKeyBindings.clear();

		for (Lane lane : getAllSupportedLanes()) {
			laneKeyBindings.put(lane, lane.getDefaultKeyCode());
		}
	}

	private void ensureLaneKeyBindings() {
		for (Lane lane : getAllSupportedLanes()) {
			laneKeyBindings.putIfAbsent(lane, lane.getDefaultKeyCode());
		}
	}

	private List<Lane> getAllSupportedLanes() {
		ArrayList<Lane> lanes = new ArrayList<>();

		for (KeyMode mode : KeyMode.values()) {
			Collections.addAll(lanes, mode.getLanes());
		}

		return lanes;
	}

	public void setLaneKeyBinding(Lane lane, int keyCode) {
		if (lane == null || keyCode == KeyEvent.VK_UNDEFINED) {
			return;
		}

		if (!isPlayableLane(lane)) {
			return;
		}

		ensureLaneKeyBindings();

		Lane existingLane = getLaneForKeyCode(keyCode);
		if (existingLane != null && existingLane != lane) {
			laneKeyBindings.put(existingLane, existingLane.getDefaultKeyCode());
		}

		laneKeyBindings.put(lane, keyCode);
	}

	public int getKeyCodeForLane(Lane lane) {
		if (lane == null) {
			return KeyEvent.VK_UNDEFINED;
		}

		ensureLaneKeyBindings();
		return laneKeyBindings.getOrDefault(lane, KeyEvent.VK_UNDEFINED);
	}

	public String getKeyTextForLane(Lane lane) {
		return KeyEvent.getKeyText(getKeyCodeForLane(lane));
	}

	public Lane getLaneForKeyCode(int keyCode) {
		ensureLaneKeyBindings();

		for (Lane lane : keyMode.getLanes()) {
			Integer boundKeyCode = laneKeyBindings.get(lane);

			if (boundKeyCode != null && boundKeyCode == keyCode) {
				return lane;
			}
		}

		return null;
	}

	public boolean isLaneKey(int keyCode) {
		return getLaneForKeyCode(keyCode) != null;
	}

	public Map<Lane, Integer> getLaneKeyBindings() {
		ensureLaneKeyBindings();

		LinkedHashMap<Lane, Integer> copy = new LinkedHashMap<>();

		for (Lane lane : keyMode.getLanes()) {
			copy.put(lane, laneKeyBindings.get(lane));
		}

		return Collections.unmodifiableMap(copy);
	}

	public void saveSettings() {
		ensureLaneKeyBindings();

		Properties props = new Properties();

		props.setProperty("globalOffset", Double.toString(GLOBAL_OFFSET));
		props.setProperty("noteIndex", Integer.toString(noteIndex));
		props.setProperty("currentDifficulty", currentDifficulty.name());
		props.setProperty("keyMode", keyMode.name());
		props.setProperty("keyCount", Integer.toString(keyMode.getKeyCount()));

		for (KeyMode mode : KeyMode.values()) {
			for (Lane lane : mode.getLanes()) {
				Integer keyCode = laneKeyBindings.get(lane);

				if (keyCode != null && keyCode != KeyEvent.VK_UNDEFINED) {
					props.setProperty(getKeyBindingPropertyName(mode, lane), Integer.toString(keyCode));
				}
			}
		}

		try {
			Path parent = settingsPath.getParent();

			if (parent != null) {
				Files.createDirectories(parent);
			}

			try (OutputStream out = Files.newOutputStream(settingsPath)) {
				props.store(out, "Java-Archive Settings");
			}
		} catch (IOException e) {
			System.err.println("Failed to save settings: " + settingsPath);
			e.printStackTrace();
		}
	}

	public void loadSettings() {
		if (!Files.exists(settingsPath)) {
			return;
		}

		Properties props = new Properties();

		try (InputStream in = Files.newInputStream(settingsPath)) {
			props.load(in);
		} catch (IOException e) {
			System.err.println("Failed to load settings: " + settingsPath);
			e.printStackTrace();
			return;
		}

		GLOBAL_OFFSET = readDouble(props, "globalOffset", GLOBAL_OFFSET);
		setNoteIndex(readInt(props, "noteIndex", noteIndex));
		currentDifficulty = readDifficulty(props, "currentDifficulty", currentDifficulty);
		keyMode = readKeyMode(props, keyMode);

		ensureLaneKeyBindings();

		for (KeyMode mode : KeyMode.values()) {
			for (Lane lane : mode.getLanes()) {
				String value = props.getProperty(getKeyBindingPropertyName(mode, lane));

				if (value == null) {
					value = props.getProperty("key." + lane.getChartToken());
				}

				if (value == null) {
					continue;
				}

				try {
					int keyCode = Integer.parseInt(value.trim());

					if (keyCode != KeyEvent.VK_UNDEFINED) {
						laneKeyBindings.put(lane, keyCode);
					}
				} catch (NumberFormatException e) {
					System.err.println("Invalid key code for " + lane.getChartToken() + ": " + value);
				}
			}
		}

		ensureLaneKeyBindings();
	}

	private String getKeyBindingPropertyName(KeyMode mode, Lane lane) {
		return "key." + mode.getKeyCount() + "." + lane.getChartToken();
	}

	private KeyMode readKeyMode(Properties props, KeyMode fallback) {
		String keyModeName = props.getProperty("keyMode");

		if (keyModeName != null && !keyModeName.trim().isEmpty()) {
			try {
				return KeyMode.valueOf(keyModeName.trim());
			} catch (IllegalArgumentException e) {
				System.err.println("Invalid key mode: " + keyModeName);
			}
		}

		int keyCount = readInt(props, "keyCount", fallback.getKeyCount());

		try {
			return KeyMode.fromKeyCount(keyCount);
		} catch (IllegalArgumentException e) {
			System.err.println("Invalid key count: " + keyCount);
			return fallback;
		}
	}

	private Difficulty readDifficulty(Properties props, String key, Difficulty fallback) {
		String value = props.getProperty(key);

		if (value == null || value.trim().isEmpty()) {
			return fallback;
		}

		try {
			return Difficulty.valueOf(value.trim());
		} catch (IllegalArgumentException e) {
			System.err.println("Invalid difficulty in settings: " + value);
			return fallback;
		}
	}

	private double readDouble(Properties props, String key, double fallback) {
		String value = props.getProperty(key);

		if (value == null || value.trim().isEmpty()) {
			return fallback;
		}

		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException e) {
			System.err.println("Invalid double setting " + key + ": " + value);
			return fallback;
		}
	}

	private int readInt(Properties props, String key, int fallback) {
		String value = props.getProperty(key);

		if (value == null || value.trim().isEmpty()) {
			return fallback;
		}

		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			System.err.println("Invalid int setting " + key + ": " + value);
			return fallback;
		}
	}
}