package state.gameplay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import asset.AssetManager;
import core.GameContext;
import stage.Stage;

public class NoteManager {

    private final GameContext context;
    private final Map<Lane, List<Note>> laneNotes = new EnumMap<>(Lane.class);

    private static final double PERFECT_WINDOW = 0.08;
    private static final double GREAT_WINDOW = 0.10;
    private static final double GOOD_WINDOW = 0.13;
    private static final double EARLY_LATE_WINDOW = 0.15;
    private static final double MISS_WINDOW = 0.20;

    private final AssetManager am = AssetManager.getInstance();

    private double bpm;
    private double offset;

    private double lastHitTimeDiffSeconds = 0.0;

    public NoteManager(GameContext context, Stage stage) {
        this(context, stage.getMusicBPM(), stage.getMusicOffsetSeconds());
    }

    public NoteManager(GameContext context, double bpm, double offset) {
        this.context = context;
        this.bpm = bpm;
        this.offset = offset;

        for (Lane lane : Lane.values()) {
            laneNotes.put(lane, new ArrayList<>());
        }
    }

    public void addNote(Note note) {
        laneNotes.get(note.getLane()).add(note);
    }

    public void loadChart(String key) {
        ArrayList<String> lines = am.getText(key);

        if (lines == null) {
            System.out.println("Chart not found: " + key);
            return;
        }

        for (Lane lane : Lane.values()) {
            laneNotes.get(lane).clear();
        }

        lastHitTimeDiffSeconds = 0.0;

        double secondsPerBeat = 60.0 / bpm;

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\s+");

            if (parts.length < 2) {
                continue;
            }

            double beat;
            Lane lane;

            try {
                beat = Double.parseDouble(parts[0]);
                lane = Lane.valueOf(parts[1]);
            } catch (Exception e) {
                continue;
            }

            double hitTime = beat * secondsPerBeat + offset;
            addNote(new Note(lane, hitTime));
        }

        for (Lane lane : Lane.values()) {
            laneNotes.get(lane).sort(Comparator.comparingDouble(Note::getHitTime));
        }

        System.out.println("Chart loaded: " + key);
    }

    public int update(double currentTime) {
        int missCount = 0;

        for (Lane lane : Lane.values()) {
            List<Note> notes = laneNotes.get(lane);

            while (!notes.isEmpty()) {
                Note note = notes.get(0);

                if (!note.isJudged() && currentTime - note.getHitTime() > MISS_WINDOW) {
                    note.judge();
                    notes.remove(0);
                    missCount++;
                } else {
                    break;
                }
            }
        }

        return missCount;
    }

    public Judgement judge(Lane lane, double currentTime) {
        List<Note> notes = laneNotes.get(lane);

        if (notes.isEmpty()) {
            return Judgement.NONE;
        }

        Note note = notes.get(0);

        double diff = currentTime - note.getHitTime();
        lastHitTimeDiffSeconds = diff;

        double absDiff = Math.abs(diff);

        if (absDiff <= PERFECT_WINDOW) {
            note.judge();
            notes.remove(0);
            return Judgement.PERFECT;
        }

        if (absDiff <= GREAT_WINDOW) {
            note.judge();
            notes.remove(0);
            return Judgement.GREAT;
        }

        if (absDiff <= GOOD_WINDOW) {
            note.judge();
            notes.remove(0);
            return Judgement.GOOD;
        }

        if (absDiff <= EARLY_LATE_WINDOW) {
            note.judge();
            notes.remove(0);
            return diff < 0 ? Judgement.EARLY : Judgement.LATE;
        }

        return Judgement.NONE;
    }

    public double getLastHitTimeDiffSeconds() {
        return lastHitTimeDiffSeconds;
    }

    public Map<Lane, List<Note>> getLaneNotes() {
        return laneNotes;
    }

    public boolean isFinished() {
        for (Lane lane : Lane.values()) {
            if (!laneNotes.get(lane).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int getRemainingNoteCount() {
        int count = 0;

        for (Lane lane : Lane.values()) {
            count += laneNotes.get(lane).size();
        }

        return count;
    }

    public double getBpm() {
        return bpm;
    }

    public double getOffset() {
        return offset;
    }
}