package state.gameplay;

public class Note {

    private static final double LONG_NOTE_EPSILON = 0.0001;

    private final Lane lane;
    private final double hitTime;
    private final double endTime;

    private boolean headJudged = false;
    private boolean active = false;
    private boolean finished = false;

    private double releaseStartTime = Double.NaN;

    public Note(Lane lane, double hitTime) {
        this(lane, hitTime, hitTime);
    }

    public Note(Lane lane, double hitTime, double endTime) {
        this.lane = lane;
        this.hitTime = hitTime;
        this.endTime = Math.max(hitTime, endTime);
    }

    public Lane getLane() {
        return lane;
    }

    public double getHitTime() {
        return hitTime;
    }

    public double getStartTime() {
        return hitTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public boolean isLongNote() {
        return endTime - hitTime > LONG_NOTE_EPSILON;
    }

    public boolean isHeadJudged() {
        return headJudged;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isJudged() {
        return finished;
    }

    public void judge() {
        judgeTap();
    }

    public void judgeTap() {
        headJudged = true;
        active = false;
        finished = true;
        releaseStartTime = Double.NaN;
    }

    public void activateLongNote() {
        headJudged = true;
        active = true;
        finished = false;
        releaseStartTime = Double.NaN;
    }

    public void finishLongNote() {
        if (!isLongNote()) {
            judgeTap();
            return;
        }

        headJudged = true;
        active = false;
        finished = true;
        releaseStartTime = Double.NaN;
    }

    public void failLongNote() {
        active = false;
        finished = true;
        releaseStartTime = Double.NaN;
    }

    public boolean hasReleaseStartTime() {
        return !Double.isNaN(releaseStartTime);
    }

    public double getReleaseStartTime() {
        return releaseStartTime;
    }

    public void markReleased(double currentTime) {
        if (Double.isNaN(releaseStartTime)) {
            releaseStartTime = currentTime;
        }
    }

    public void clearReleaseStartTime() {
        releaseStartTime = Double.NaN;
    }
}