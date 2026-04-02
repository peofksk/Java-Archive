package state.gameplay;

import java.awt.event.KeyEvent;

public enum Lane {
	D("Left 1", KeyEvent.VK_D),
	F("Left 2", KeyEvent.VK_F),
	J("Right 1", KeyEvent.VK_J),
	K("Right 2", KeyEvent.VK_K);

	private final String displayName;
	private final int defaultKeyCode;

	Lane(String displayName, int defaultKeyCode) {
		this.displayName = displayName;
		this.defaultKeyCode = defaultKeyCode;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getDefaultKeyCode() {
		return defaultKeyCode;
	}
}