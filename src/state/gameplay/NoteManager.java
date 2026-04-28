package state.gameplay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import asset.AssetManager;
import core.GameContext;
import stage.Stage;

public class NoteManager {

	private static final double PERFECT_WINDOW = 0.08;
	private static final double GREAT_WINDOW = 0.10;
	private static final double GOOD_WINDOW = 0.13;
	private static final double EARLY_LATE_WINDOW = 0.15;
	private static final double MISS_WINDOW = 0.20;
	private static final double HOLD_BREAK_TOLERANCE = 0.10;

	private final GameContext context;
	private final Map<Lane, List<Note>> laneNotes = new EnumMap<>(Lane.class);
	private final Map<Lane, Note> activeLongNotes = new EnumMap<>(Lane.class);
	private final List<Judgement> pendingJudgements = new ArrayList<>();

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
		activeLongNotes.clear();
		pendingJudgements.clear();

		lastHitTimeDiffSeconds = 0.0;
		double secondsPerBeat = 60.0 / bpm;

		for (String rawLine : lines) {
			if (rawLine == null) {
				continue;
			}

			String line = rawLine.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}

			String[] parts = line.split("\\s+");
			if (parts.length < 2) {
				continue;
			}

			double startBeat;
			try {
				startBeat = Double.parseDouble(parts[0]);
			} catch (NumberFormatException e) {
				continue;
			}

			double endBeat = startBeat;
			int laneTokenStartIndex = 1;

			if (parts.length >= 3) {
				try {
					endBeat = Double.parseDouble(parts[1]);
					laneTokenStartIndex = 2;
				} catch (NumberFormatException ignored) {
					endBeat = startBeat;
					laneTokenStartIndex = 1;
				}
			}

			if (laneTokenStartIndex >= parts.length) {
				continue;
			}

			String laneToken = String.join(" ", java.util.Arrays.copyOfRange(parts, laneTokenStartIndex, parts.length));
			Lane lane = Lane.fromChartToken(laneToken);
			if (lane == null || !context.isPlayableLane(lane)) {
				continue;
			}

			double hitTime = startBeat * secondsPerBeat + offset;
			double endTime = Math.max(startBeat, endBeat) * secondsPerBeat + offset;
			addNote(new Note(lane, hitTime, endTime));
		}

		for (Lane lane : context.getPlayableLanes()) {
			laneNotes.get(lane).sort(Comparator.comparingDouble(Note::getHitTime));
		}
	}

	public int update(double currentTime, Map<Lane, Boolean> laneHeldStates) {
		int missCount = 0;

		for (Lane lane : context.getPlayableLanes()) {
			List<Note> notes = laneNotes.get(lane);
			if (notes == null) {
				continue;
			}

			while (!notes.isEmpty()) {
				Note note = notes.get(0);

				if (currentTime - note.getHitTime() > MISS_WINDOW) {
					note.failLongNote();
					notes.remove(0);
					missCount++;
				} else {
					break;
				}
			}
		}

		for (Lane lane : context.getPlayableLanes()) {
			Note activeLongNote = activeLongNotes.get(lane);
			if (activeLongNote == null) {
				continue;
			}

			boolean laneHeld = laneHeldStates != null && laneHeldStates.getOrDefault(lane, false);

			if (laneHeld) {
				activeLongNote.clearReleaseStartTime();
			} else {
				activeLongNote.markReleased(currentTime);
			}

			boolean longNoteEnded = currentTime >= activeLongNote.getEndTime();
			boolean holdBroken = activeLongNote.hasReleaseStartTime()
					&& (currentTime - activeLongNote.getReleaseStartTime()) > HOLD_BREAK_TOLERANCE;

			if (!longNoteEnded && holdBroken) {
				activeLongNote.failLongNote();
				activeLongNotes.remove(lane);
				missCount++;
				continue;
			}

			if (longNoteEnded) {
				boolean releasedTooEarly = activeLongNote.hasReleaseStartTime()
						&& (activeLongNote.getEndTime() - activeLongNote.getReleaseStartTime()) > HOLD_BREAK_TOLERANCE;

				if (releasedTooEarly) {
					activeLongNote.failLongNote();
					activeLongNotes.remove(lane);
					missCount++;
					continue;
				}

				if (currentTime - activeLongNote.getEndTime() > EARLY_LATE_WINDOW) {
					activeLongNote.finishLongNote();
					activeLongNotes.remove(lane);
					pendingJudgements.add(Judgement.PERFECT);
				}
			}
		}

		return missCount;
	}

	public Judgement judge(Lane lane, double currentTime) {
		if (lane == null) {
			return Judgement.NONE;
		}

		if (activeLongNotes.containsKey(lane)) {
			return Judgement.NONE;
		}

		List<Note> notes = laneNotes.get(lane);
		if (notes == null || notes.isEmpty()) {
			return Judgement.NONE;
		}

		Note note = notes.get(0);
		double diff = currentTime - note.getHitTime();
		lastHitTimeDiffSeconds = diff;

		double absDiff = Math.abs(diff);

		if (absDiff <= PERFECT_WINDOW) {
			return finalizeJudgement(note, notes, Judgement.PERFECT);
		}

		if (absDiff <= GREAT_WINDOW) {
			return finalizeJudgement(note, notes, Judgement.GREAT);
		}

		if (absDiff <= GOOD_WINDOW) {
			return finalizeJudgement(note, notes, Judgement.GOOD);
		}

		if (absDiff <= EARLY_LATE_WINDOW) {
			return finalizeJudgement(note, notes, diff < 0 ? Judgement.EARLY : Judgement.LATE);
		}

		return Judgement.NONE;
	}

	public Judgement judgeLongNoteEnd(Lane lane, double currentTime) {
		Note note = activeLongNotes.get(lane);
		if (note == null) {
			return Judgement.NONE;
		}

		double diff = currentTime - note.getEndTime();
		double absDiff = Math.abs(diff);

		if (absDiff <= PERFECT_WINDOW) {
			note.finishLongNote();
			activeLongNotes.remove(lane);
			return Judgement.PERFECT;
		}
		if (absDiff <= GREAT_WINDOW) {
			note.finishLongNote();
			activeLongNotes.remove(lane);
			return Judgement.GREAT;
		}
		if (absDiff <= GOOD_WINDOW) {
			note.finishLongNote();
			activeLongNotes.remove(lane);
			return Judgement.GOOD;
		}
		if (absDiff <= EARLY_LATE_WINDOW) {
			note.finishLongNote();
			activeLongNotes.remove(lane);
			return diff < 0 ? Judgement.EARLY : Judgement.LATE;
		}

		if (diff < -EARLY_LATE_WINDOW) {
			note.failLongNote();
			activeLongNotes.remove(lane);
			return Judgement.MISS;
		}

		note.finishLongNote();
		activeLongNotes.remove(lane);
		return Judgement.PERFECT;
	}

	private Judgement finalizeJudgement(Note note, List<Note> notes, Judgement judgement) {
		if (note.isLongNote()) {
			note.activateLongNote();
			activeLongNotes.put(note.getLane(), note);
		} else {
			note.judgeTap();
		}

		notes.remove(0);
		return judgement;
	}

	public List<Judgement> drainPendingJudgements() {
		List<Judgement> result = new ArrayList<>(pendingJudgements);
		pendingJudgements.clear();
		return result;
	}

	public double getLastHitTimeDiffSeconds() {
		return lastHitTimeDiffSeconds;
	}

	public Map<Lane, List<Note>> getLaneNotes() {
		return laneNotes;
	}

	public Note getActiveLongNote(Lane lane) {
		return activeLongNotes.get(lane);
	}

	public boolean hasActiveLongNote(Lane lane) {
		return activeLongNotes.containsKey(lane);
	}

	public Collection<Note> getActiveLongNotes() {
		return Collections.unmodifiableCollection(activeLongNotes.values());
	}

	public boolean isFinished() {
		if (!activeLongNotes.isEmpty()) {
			return false;
		}

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

		for (Note note : activeLongNotes.values()) {
			count += note.isLongNote() ? 1 : 1;
		}

		for (Lane lane : context.getPlayableLanes()) {
			List<Note> notes = laneNotes.get(lane);
			if (notes != null) {
				for (Note note : notes) {
					count += note.isLongNote() ? 2 : 1;
				}
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