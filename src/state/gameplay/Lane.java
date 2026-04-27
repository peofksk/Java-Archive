package state.gameplay;

import java.awt.event.KeyEvent;

public enum Lane {
	LEFT_1("Left 1", KeyEvent.VK_D), LEFT_2("Left 2", KeyEvent.VK_F), RIGHT_1("Right 1", KeyEvent.VK_J),
	RIGHT_2("Right 2", KeyEvent.VK_K);

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

	public static Lane fromChartToken(String token) {
		if (token == null) {
			return null;
		}

		String normalized = token.trim();
		if (normalized.isEmpty()) {
			return null;
		}

		for (Lane lane : values()) {
			if (lane.name().equalsIgnoreCase(normalized) || lane.displayName.equalsIgnoreCase(normalized)) {
				return lane;
			}
		}

		return null;
	}
}
