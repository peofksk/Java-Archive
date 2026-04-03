package state.gameplay;

import java.awt.event.KeyEvent;

public enum Lane {
    LEFT_1("Left 1", KeyEvent.VK_D, 0),
    LEFT_2("Left 2", KeyEvent.VK_F, 1),
    RIGHT_1("Right 1", KeyEvent.VK_J, 2),
    RIGHT_2("Right 2", KeyEvent.VK_K, 3);

    private final String displayName;
    private final int defaultKeyCode;
    private final int index;

    Lane(String displayName, int defaultKeyCode, int index) {
        this.displayName = displayName;
        this.defaultKeyCode = defaultKeyCode;
        this.index = index;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultKeyCode() {
        return defaultKeyCode;
    }

    public int getIndex() {
        return index;
    }

    public static Lane fromIndex(int index) {
        for (Lane lane : values()) {
            if (lane.index == index) {
                return lane;
            }
        }
        return null;
    }

    public static Lane fromChartToken(String token) {
        if (token == null) {
            return null;
        }

        String normalized = token.trim();

        for (Lane lane : values()) {
            if (lane.displayName.equalsIgnoreCase(normalized)) {
                return lane;
            }
        }

        return null;
    }
}