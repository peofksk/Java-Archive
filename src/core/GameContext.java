package core;

import audio.BGMManager;
import stage.Difficulty;
import stage.StageManager;

public class GameContext {
    public BGMManager bgm = new BGMManager();
    public StageManager sm = new StageManager();

    private GameState currentState;
    private Difficulty currentDifficulty = Difficulty.Easy;
    private double GLOBAL_OFFSET = 0.0;

    private final int NOTE_COUNT = 12;
    private int noteIndex = 0;

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
}