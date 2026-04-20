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

    private final double bpm;
    private final double offset;

    private double lastHitTimeDiffSeconds = 0.0;

    public NoteManager(GameContext context, Stage stage) {
        this(context, stage.getMusicBPM(), stage.getMusicOffsetSeconds());
    }

    public NoteManager(GameContext context, double bpm, double offset) {
        this.context = context;
        this.bpm = bpm;
        this.offset = offset;

        for (Lane lane : context.getPlayableLanes()) {
            laneNotes.put(lane, new ArrayList<>());
        }
    }

    public void addNote(Note note) {
        List<Note> notes = laneNotes.get(note.getLane());
        if (notes != null) {
            notes.add(note);
        }
    }

    public void loadChart(String key) {
        ArrayList<String> lines = am.getText(key);

        if (lines == null) {
            System.out.println("Chart not found: " + key);
            return;
        }

        for (Lane lane : context.getPlayableLanes()) {
            laneNotes.computeIfAbsent(lane, unused -> new ArrayList<>()).clear();
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
            try {
                beat = Double.parseDouble(parts[0]);
            } catch (Exception e) {
                continue;
            }

            String laneToken = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            Lane lane = Lane.fromChartToken(laneToken);
            if (lane == null || !context.isPlayableLane(lane)) {
                continue;
            }

            double hitTime = beat * secondsPerBeat + offset;
            addNote(new Note(lane, hitTime));
        }

        for (Lane lane : context.getPlayableLanes()) {
            laneNotes.get(lane).sort(Comparator.comparingDouble(Note::getHitTime));
        }

        System.out.println("Chart loaded: " + key);
    }

    public int update(double currentTime) {
        int missCount = 0;

        for (Lane lane : context.getPlayableLanes()) {
            List<Note> notes = laneNotes.get(lane);
            if (notes == null) {
                continue;
            }

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
        if (notes == null || notes.isEmpty()) {
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
        for (Lane lane : context.getPlayableLanes()) {
            List<Note> notes = laneNotes.get(lane);
            if (notes != null && !notes.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int getRemainingNoteCount() {
        int count = 0;

        for (Lane lane : context.getPlayableLanes()) {
            List<Note> notes = laneNotes.get(lane);
            if (notes != null) {
                count += notes.size();
            }
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
