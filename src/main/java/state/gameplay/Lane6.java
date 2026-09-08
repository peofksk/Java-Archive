package state.gameplay;

import java.awt.event.KeyEvent;

public enum Lane6 implements Lane {
	LEFT_1("Left 1", KeyEvent.VK_S),
	LEFT_2("Left 2", KeyEvent.VK_D),
	LEFT_3("Left 3", KeyEvent.VK_F),
	RIGHT_1("Right 1", KeyEvent.VK_J),
	RIGHT_2("Right 2", KeyEvent.VK_K),
	RIGHT_3("Right 3", KeyEvent.VK_L);

	private final String displayName;
	private final int defaultKeyCode;

	Lane6(String displayName, int defaultKeyCode) {
		this.displayName = displayName;
		this.defaultKeyCode = defaultKeyCode;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public int getDefaultKeyCode() {
		return defaultKeyCode;
	}

	@Override
	public String getChartToken() {
		return name();
	}
}