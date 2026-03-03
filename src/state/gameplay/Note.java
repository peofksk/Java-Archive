package state.gameplay;

public class Note {

    private final Lane lane;
    private final double hitTime;

    private boolean judged = false;

    public Note(Lane lane, double hitTime) {
        this.lane = lane;
        this.hitTime = hitTime;
    }

    public Lane getLane() {
        return lane;
    }

    public double getHitTime() {
        return hitTime;
    }

    public boolean isJudged() {
        return judged;
    }

    public void judge() {
        judged = true;
    }
}