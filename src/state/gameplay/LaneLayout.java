package state.gameplay;

import java.util.List;

public class LaneLayout {
    private final List<Lane> lanes;
    private final int startX;
    private final int totalWidth;
    private final int laneHeight;

    public LaneLayout(List<Lane> lanes, int startX, int totalWidth, int laneHeight) {
        if (lanes == null || lanes.isEmpty()) {
            throw new IllegalArgumentException("lanes must not be empty");
        }

        this.lanes = lanes;
        this.startX = startX;
        this.totalWidth = totalWidth;
        this.laneHeight = laneHeight;
    }

    public List<Lane> getLanes() {
        return lanes;
    }

    public int getLaneCount() {
        return lanes.size();
    }

    public int getStartX() {
        return startX;
    }

    public int getTotalWidth() {
        return totalWidth;
    }

    public int getLaneHeight() {
        return laneHeight;
    }

    public int getEndX() {
        return startX + totalWidth;
    }

    public int getLaneIndex(Lane lane) {
        return lanes.indexOf(lane);
    }

    public int getLaneX(int laneIndex) {
        return startX + (int) Math.round((double) totalWidth * laneIndex / getLaneCount());
    }

    public int getLaneWidth(int laneIndex) {
        return getLaneX(laneIndex + 1) - getLaneX(laneIndex);
    }

    public int getLaneX(Lane lane) {
        int index = getLaneIndex(lane);
        if (index < 0) {
            return startX;
        }
        return getLaneX(index);
    }

    public int getLaneWidth(Lane lane) {
        int index = getLaneIndex(lane);
        if (index < 0) {
            return totalWidth;
        }
        return getLaneWidth(index);
    }

    public int getMinLaneWidth() {
        int minWidth = Integer.MAX_VALUE;
        for (int i = 0; i < getLaneCount(); i++) {
            minWidth = Math.min(minWidth, getLaneWidth(i));
        }
        return minWidth;
    }
}
