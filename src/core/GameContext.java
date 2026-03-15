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
}