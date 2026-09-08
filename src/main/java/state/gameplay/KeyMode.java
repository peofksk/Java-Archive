package state.gameplay;

public enum KeyMode {
	KEY_4(4, Lane4.values()),
	KEY_6(6, Lane6.values());

	private final int keyCount;
	private final Lane[] lanes;

	KeyMode(int keyCount, Lane[] lanes) {
		this.keyCount = keyCount;
		this.lanes = lanes;
	}

	public int getKeyCount() {
		return keyCount;
	}

	public Lane[] getLanes() {
		return lanes.clone();
	}

	public Lane fromChartToken(String token) {
		if (token == null) {
			return null;
		}

		String normalized = token.trim();
		if (normalized.isEmpty()) {
			return null;
		}

		for (Lane lane : lanes) {
			if (lane.getChartToken().equalsIgnoreCase(normalized)
					|| lane.getDisplayName().equalsIgnoreCase(normalized)) {
				return lane;
			}
		}

		return null;
	}

	public static KeyMode fromKeyCount(int keyCount) {
		for (KeyMode mode : values()) {
			if (mode.keyCount == keyCount) {
				return mode;
			}
		}

		throw new IllegalArgumentException("Unsupported key count: " + keyCount);
	}
}