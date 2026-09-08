package state.gameplay;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import core.GameContext;

public class AutoRobot {
    private static final double TAP_RELEASE_DELAY_SECONDS = 0.035;
    private static final double EVENT_EPSILON = 0.000001;

    private final GamePlayState gamePlayState;
    private final GameContext context;
    private final ArrayList<AutoAction> actions = new ArrayList<>();

    private int nextActionIndex = 0;
    private boolean finished = false;

    public AutoRobot(GamePlayState gamePlayState, GameContext context, NoteManager noteManager) {
        this.gamePlayState = gamePlayState;
        this.context = context;
        buildActions(noteManager);
    }

    private void buildActions(NoteManager noteManager) {
        actions.clear();
        nextActionIndex = 0;
        finished = false;

        if (noteManager == null) {
            finished = true;
            return;
        }

        Map<Lane, List<Note>> laneNotes = noteManager.getLaneNotes();

        for (Map.Entry<Lane, List<Note>> entry : laneNotes.entrySet()) {
            Lane lane = entry.getKey();
            List<Note> notes = entry.getValue();

            if (lane == null || notes == null) {
                continue;
            }

            for (Note note : notes) {
                if (note == null) {
                    continue;
                }

                actions.add(new AutoAction(note.getHitTime(), lane, KeyEvent.KEY_PRESSED));

                if (note.isLongNote()) {
                    actions.add(new AutoAction(note.getEndTime(), lane, KeyEvent.KEY_RELEASED));
                } else {
                    actions.add(new AutoAction(
                            note.getHitTime() + TAP_RELEASE_DELAY_SECONDS,
                            lane,
                            KeyEvent.KEY_RELEASED
                    ));
                }
            }
        }

        actions.sort(
                Comparator.comparingDouble(AutoAction::time)
                        .thenComparingInt(AutoAction::priority)
        );

        if (actions.isEmpty()) {
            finished = true;
        }
    }

    public void update(double gameTime) {
        if (finished) {
            return;
        }

        while (nextActionIndex < actions.size()) {
            AutoAction action = actions.get(nextActionIndex);

            if (action.time() > gameTime + EVENT_EPSILON) {
                break;
            }

            dispatch(action);
            nextActionIndex++;
        }

        if (nextActionIndex >= actions.size()) {
            finished = true;
        }
    }

    private void dispatch(AutoAction action) {
        int keyCode = context.getKeyCodeForLane(action.lane());

        if (keyCode == KeyEvent.VK_UNDEFINED) {
            return;
        }

        if (action.eventId() == KeyEvent.KEY_PRESSED) {
            gamePlayState.dispatchAutoKeyPressed(keyCode);
        } else if (action.eventId() == KeyEvent.KEY_RELEASED) {
            gamePlayState.dispatchAutoKeyReleased(keyCode);
        }
    }

    public boolean isFinished() {
        return finished;
    }

    private record AutoAction(double time, Lane lane, int eventId) {
        int priority() {
            if (eventId == KeyEvent.KEY_RELEASED) {
                return 0;
            }

            return 1;
        }
    }
}