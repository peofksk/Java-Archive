package core;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
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
    private double GLOBAL_OFFSET = -0.08;

    private final EnumMap<Lane, Integer> laneKeyBindings = new EnumMap<>(Lane.class);
    private final ArrayList<Lane> playableLanes = new ArrayList<>();

    private final int NOTE_COUNT = 12;
    private int noteIndex = 0;

    public GameContext() {
        resetPlayableLanesToDefault();
        resetLaneKeyBindingsToDefault();
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
}
