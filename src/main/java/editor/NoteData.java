package editor;

class NoteData {
    int laneIndex;
    double beat;
    double endBeat;

    public NoteData(int laneIndex, double beat) {
        this(laneIndex, beat, beat);
    }

    public NoteData(int laneIndex, double beat, double endBeat) {
        this.laneIndex = laneIndex;
        this.beat = beat;
        this.endBeat = Math.max(beat, endBeat);
    }

    public boolean isLongNote() {
        return endBeat - beat > 0.0001;
    }

    public boolean containsBeat(double value, double epsilon) {
        if (Math.abs(beat - value) < epsilon) {
            return true;
        }

        return isLongNote() && value > beat - epsilon && value < endBeat + epsilon;
    }

    public boolean overlaps(double start, double end) {
        double noteStart = beat;
        double noteEnd = endBeat;
        return Math.max(noteStart, start) <= Math.min(noteEnd, end) + 0.0001;
    }
}