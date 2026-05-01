package core;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import audio.BGMManager;
import stage.Difficulty;
import stage.StageManager;
import state.gameplay.Lane;

public class GameContext {
	public BGMManager bgm = new BGMManager();
	public StageManager sm = new StageManager();

	private static final String SETTINGS_DIRECTORY_NAME = ".Java-Archive";
	private static final String SETTINGS_FILE_NAME = "settings.properties";
	private static final String PLAYABLE_LANE_SEPARATOR = ",";

	private final Path settingsPath = Paths.get(
			System.getProperty("user.home"),
			SETTINGS_DIRECTORY_NAME,
			SETTINGS_FILE_NAME
	);

	private GameState currentState;
	private Difficulty currentDifficulty = Difficulty.Easy;
	private double GLOBAL_OFFSET = -0.08;

	private final EnumMap<Lane, Integer> laneKeyBindings = new EnumMap<>(Lane.class);
	private final ArrayList<Lane> playableLanes = new ArrayList<>();

	private final int NOTE_COUNT = 12;
	private int noteIndex = 0;

	public GameContext() {
		resetPlayableLanesToDefault();
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

	public void resetPlayableLanesToDefault() {
		setPlayableLanes(Arrays.asList(Lane.values()));
	}

	public void setPlayableLanes(List<Lane> lanes) {
		LinkedHashSet<Lane> normalized = new LinkedHashSet<>();

		if (lanes != null) {
			for (Lane lane : lanes) {
				if (lane != null) {
					normalized.add(lane);
				}
			}
		}

		if (normalized.isEmpty()) {
			normalized.addAll(Arrays.asList(Lane.values()));
		}

		playableLanes.clear();
		playableLanes.addAll(normalized);
		ensureLaneKeyBindings();
	}

	public List<Lane> getPlayableLanes() {
		return Collections.unmodifiableList(playableLanes);
	}

	public int getLaneCount() {
		return playableLanes.size();
	}

	public boolean isPlayableLane(Lane lane) {
		return playableLanes.contains(lane);
	}

	public int getLaneIndex(Lane lane) {
		return playableLanes.indexOf(lane);
	}

	public void resetLaneKeyBindingsToDefault() {
		laneKeyBindings.clear();
		for (Lane lane : Lane.values()) {
			laneKeyBindings.put(lane, lane.getDefaultKeyCode());
		}
	}

	private void ensureLaneKeyBindings() {
		for (Lane lane : Lane.values()) {
			laneKeyBindings.putIfAbsent(lane, lane.getDefaultKeyCode());
		}
	}

	public void setLaneKeyBinding(Lane lane, int keyCode) {
		if (lane == null || keyCode == KeyEvent.VK_UNDEFINED) {
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

		for (Lane lane : playableLanes) {
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
		EnumMap<Lane, Integer> copy = new EnumMap<>(Lane.class);
		for (Lane lane : playableLanes) {
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
		props.setProperty("playableLanes", encodePlayableLanes());

		for (Lane lane : Lane.values()) {
			Integer keyCode = laneKeyBindings.get(lane);
			if (keyCode != null && keyCode != KeyEvent.VK_UNDEFINED) {
				props.setProperty("key." + lane.name(), Integer.toString(keyCode));
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

		List<Lane> loadedPlayableLanes = readPlayableLanes(props, "playableLanes");
		if (!loadedPlayableLanes.isEmpty()) {
			setPlayableLanes(loadedPlayableLanes);
		}

		for (Lane lane : Lane.values()) {
			String value = props.getProperty("key." + lane.name());
			if (value == null) {
				continue;
			}

			try {
				int keyCode = Integer.parseInt(value.trim());
				if (keyCode != KeyEvent.VK_UNDEFINED) {
					laneKeyBindings.put(lane, keyCode);
				}
			} catch (NumberFormatException e) {
				System.err.println("Invalid key code for " + lane.name() + ": " + value);
			}
		}

		ensureLaneKeyBindings();
	}

	private String encodePlayableLanes() {
		StringBuilder builder = new StringBuilder();

		for (Lane lane : playableLanes) {
			if (builder.length() > 0) {
				builder.append(PLAYABLE_LANE_SEPARATOR);
			}
			builder.append(lane.name());
		}

		return builder.toString();
	}

	private List<Lane> readPlayableLanes(Properties props, String key) {
		String value = props.getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			return Collections.emptyList();
		}

		ArrayList<Lane> lanes = new ArrayList<>();
		String[] tokens = value.split(PLAYABLE_LANE_SEPARATOR);

		for (String token : tokens) {
			if (token == null) {
				continue;
			}

			String laneName = token.trim();
			if (laneName.isEmpty()) {
				continue;
			}

			try {
				lanes.add(Lane.valueOf(laneName));
			} catch (IllegalArgumentException e) {
				System.err.println("Invalid playable lane in settings: " + laneName);
			}
		}

		return lanes;
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