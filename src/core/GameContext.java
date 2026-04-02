package core;

import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import audio.BGMManager;
import stage.Difficulty;
import stage.StageManager;
import state.gameplay.Lane;

public class GameContext {
	public BGMManager bgm = new BGMManager();
	public StageManager sm = new StageManager();

	private GameState currentState;
	private Difficulty currentDifficulty = Difficulty.Easy;
	private double GLOBAL_OFFSET = 0.0;

	private final EnumMap<Lane, Integer> laneKeyBindings = new EnumMap<>(Lane.class);

	private final int NOTE_COUNT = 12;
	private int noteIndex = 0;

	public GameContext() {
		resetLaneKeyBindingsToDefault();
	}

	public void changeState(GameState next) {
		if (currentState != null)
			currentState.exit();
		currentState = next;
		if (currentState != null)
			currentState.enter();
	}

	public GameState getCurrentState() {
		return currentState;
	}

	public Difficulty getCurrentDifficulty() {
		return currentDifficulty;
	}

	public void setCurrentDifficulty(Difficulty currentDifficulty) {
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

	public void resetLaneKeyBindingsToDefault() {
		laneKeyBindings.clear();

		for (Lane lane : Lane.values()) {
			laneKeyBindings.put(lane, lane.getDefaultKeyCode());
		}
	}

	public void setLaneKeyBinding(Lane lane, int keyCode) {
		if (lane == null || keyCode == KeyEvent.VK_UNDEFINED) {
			return;
		}

		Lane existingLane = getLaneForKeyCode(keyCode);
		if (existingLane != null && existingLane != lane) {
			laneKeyBindings.put(existingLane, lane.getDefaultKeyCode());
		}

		laneKeyBindings.put(lane, keyCode);
	}

	public int getKeyCodeForLane(Lane lane) {
		if (lane == null) {
			return KeyEvent.VK_UNDEFINED;
		}

		return laneKeyBindings.getOrDefault(lane, KeyEvent.VK_UNDEFINED);
	}

	public String getKeyTextForLane(Lane lane) {
		return KeyEvent.getKeyText(getKeyCodeForLane(lane));
	}

	public Lane getLaneForKeyCode(int keyCode) {
		for (Map.Entry<Lane, Integer> entry : laneKeyBindings.entrySet()) {
			Integer boundKeyCode = entry.getValue();
			if (boundKeyCode != null && boundKeyCode == keyCode) {
				return entry.getKey();
			}
		}
		return null;
	}

	public boolean isLaneKey(int keyCode) {
		return getLaneForKeyCode(keyCode) != null;
	}

	public Map<Lane, Integer> getLaneKeyBindings() {
		return Collections.unmodifiableMap(laneKeyBindings);
	}
}