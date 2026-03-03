package state.gameplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NoteManager {

    private final Map<Lane, List<Note>> laneNotes = new HashMap<>();

    private static final double PERFECT_WINDOW = 0.08;
    private static final double MISS_WINDOW = 0.20;

    public NoteManager() {
        for (Lane lane : Lane.values()) {
            laneNotes.put(lane, new ArrayList<>());
        }
    }

    public void addNote(Note note) {
        laneNotes.get(note.getLane()).add(note);
    }

    public void update(double currentTime) {

        for (Lane lane : Lane.values()) {

            Iterator<Note> it = laneNotes.get(lane).iterator();

            while (it.hasNext()) {
                Note note = it.next();

                if (!note.isJudged()
                        && currentTime - note.getHitTime() > MISS_WINDOW) {

                    it.remove(); // miss
                }
            }
        }
    }

    public boolean judge(Lane lane, double currentTime) {

        List<Note> notes = laneNotes.get(lane);

        if (notes.isEmpty()) return false;

        Note note = notes.get(0);

        double diff = Math.abs(currentTime - note.getHitTime());

        if (diff <= PERFECT_WINDOW) {
            note.judge();
            notes.remove(0);
            return true;
        }

        return false;
    }

    public Map<Lane, List<Note>> getLaneNotes() {
        return laneNotes;
    }
}