package core;

import audio.BGMManager;

public class GameContext {
    public BGMManager bgm = new BGMManager();
    private GameState currentState;
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

}