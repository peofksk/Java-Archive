/******************************
package state;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import stage.CorrectionConfig;
import state.gameplay.Judgement;
import state.gameplay.Lane;
import state.gameplay.LaneLayout;
import state.gameplay.Note;
import state.gameplay.NoteManager;
import util.RenderUtils;

public class CorrectionState implements GameState {

    private static final int SCREEN_WIDTH = 1024;
    private static final int SCREEN_HEIGHT = 576;
    private static final int PLAYFIELD_START_X = 30;
    private static final int PLAYFIELD_WIDTH = 320;
    private static final int JUDGEMENT_LINE_Y = 383;
    private static final double LEAD_IN = 3.0;
    private static final double AUDIO_OUTPUT_LATENCY = 0.2;
    private static final double NOTE_SCROLL_SPEED = 800.0;

    private static final double OFFSET_SEARCH_MIN_SECONDS = -2.0;
    private static final double OFFSET_SEARCH_MAX_SECONDS = 2.0;
    private static final double OFFSET_SEARCH_COARSE_STEP_SECONDS = 0.005;
    private static final double OFFSET_SEARCH_FINE_STEP_SECONDS = 0.0005;
    private static final double TAP_MATCH_TOLERANCE_SECONDS = 0.12;

    private final GameContext context;
    private final CorrectionConfig correctionConfig;
    private final AssetManager am = AssetManager.getInstance();

    private NoteManager nm;

    private Image background;
    private Image judgementLine;
    private Image noteImage;

    private boolean preloaded = false;
    private volatile boolean musicThreadStarted = false;
    private volatile boolean audioStarted = false;

    private volatile long songStartNano = 0L;
    private double timelineTime = -LEAD_IN;

    private String lastJudge = "";
    private float judgeAlpha = 0.0f;
    private double lastDiffMs = 0.0;

    private long hitCount = 0;
    private double sumAbsDiffMs = 0.0;
    private double sumSignedDiffMs = 0.0;

    private boolean resultMode = false;
    private boolean offsetApplied = false;

    private double estimatedOffsetMs = 0.0;
    private double residualAbsMs = 0.0;

    private final ArrayList<Double> tapTimes = new ArrayList<>();
    private final ArrayList<Double> chartHitTimes = new ArrayList<>();

    private final EnumMap<Lane, Boolean> lanePressed = new EnumMap<>(Lane.class);

    public CorrectionState(GameContext context, CorrectionConfig correctionConfig) {
        this.context = context;
        this.correctionConfig = correctionConfig;
        initializeLanePressedMap();
    }

    @Override
    public void enter() {
        resetRunState();
        context.bgm.stop();

        if (!preloaded) {
            preload();
        } else {
            nm = new NoteManager(context, correctionConfig.getMusicBPM(), correctionConfig.getMusicOffset());
            nm.loadChart(correctionConfig.getNoteFilePath());
            rebuildChartHitTimeline();
            context.bgm.load(correctionConfig.getMusicPath());
        }

        long now = System.nanoTime();
        songStartNano = now + (long) (LEAD_IN * 1_000_000_000L);

        audioStarted = false;
        musicThreadStarted = false;
        timelineTime = -LEAD_IN;

        startScheduledMusicThread();
    }

    @Override
    public void update(double deltaTime) {
        updateTimelineTime();
        double gameTime = getGameplayTime();

        if (!resultMode) {
            nm.update(gameTime, lanePressed);

            if (audioStarted && !context.bgm.isPlaying() && timelineTime > 0 && nm.isFinished()) {
                resultMode = true;
                applyOffsetOnce();
            }
        }

        judgeAlpha = Math.max(0.0f, judgeAlpha - 0.02f);
    }

    @Override
    public void render(Graphics2D g) {
        if (!resultMode) {
            updateTimelineTime();
        }

        LaneLayout layout = getLaneLayout();

        if (background != null) {
            g.drawImage(background, 0, 0, null);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        }

        drawLane(g, layout);
        drawJudgementLine(g, layout);
        drawLaneKeyBindings(g, layout);

        if (!resultMode) {
            renderNotes(g, layout, getGameplayTime());
            renderHud(g);
        } else {
            renderResult(g);
        }
    }

    @Override
    public void exit() {
        context.bgm.stop();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_ESCAPE) {
            context.changeState(new LevelSelectState(context));
            return;
        }

        if (resultMode) {
            if (key == KeyEvent.VK_ENTER) {
                retry();
            }
            return;
        }

        updateTimelineTime();

        double judgeTime = getGameplayTime();
        Judgement result = Judgement.NONE;

        Lane inputLane = context.getLaneForKeyCode(key);
        if (inputLane != null) {
            if (isLanePressed(inputLane)) {
                return;
            }

            setLanePressed(inputLane, true);
            tapTimes.add(judgeTime);

            result = nm.judge(inputLane, judgeTime);
            showTapFeedback(result, judgeTime);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Lane inputLane = context.getLaneForKeyCode(e.getKeyCode());
        if (inputLane != null) {
            setLanePressed(inputLane, false);
        }
    }

    public void preload() {
        if (preloaded) {
            return;
        }

        background = am.getImage(correctionConfig.getBackgroundImageKey());
        judgementLine = am.getImage("judgement_line");
        noteImage = am.getImage("note_" + context.getNoteIndex());

        nm = new NoteManager(context, correctionConfig.getMusicBPM(), correctionConfig.getMusicOffset());
        nm.loadChart(correctionConfig.getNoteFilePath());
        rebuildChartHitTimeline();

        context.bgm.load(correctionConfig.getMusicPath());
        preloaded = true;
    }

    private void rebuildChartHitTimeline() {
        chartHitTimes.clear();

        for (List<Note> notes : nm.getLaneNotes().values()) {
            for (Note note : notes) {
                chartHitTimes.add(note.getHitTime());
            }
        }

        chartHitTimes.sort(Comparator.naturalOrder());
    }

    private LaneLayout getLaneLayout() {
        return new LaneLayout(context.getPlayableLanes(), PLAYFIELD_START_X, PLAYFIELD_WIDTH, SCREEN_HEIGHT);
    }

    private void updateTimelineTime() {
        timelineTime = getScheduledPlaybackTime();
    }

    private double getScheduledPlaybackTime() {
        long now = System.nanoTime();
        return ((now - songStartNano) / 1_000_000_000.0) + AUDIO_OUTPUT_LATENCY;
    }

    private double getGameplayTime() {
        return timelineTime + context.getGlobalOffset();
    }

    private void retry() {
        context.bgm.stop();
        resetRunState();

        nm = new NoteManager(context, correctionConfig.getMusicBPM(), correctionConfig.getMusicOffset());
        nm.loadChart(correctionConfig.getNoteFilePath());
        rebuildChartHitTimeline();

        long now = System.nanoTime();
        songStartNano = now + (long) (LEAD_IN * 1_000_000_000L);

        audioStarted = false;
        musicThreadStarted = false;
        timelineTime = -LEAD_IN;

        startScheduledMusicThread();
    }

    private void resetRunState() {
        lastJudge = "";
        judgeAlpha = 0.0f;
        lastDiffMs = 0.0;
        hitCount = 0;
        sumAbsDiffMs = 0.0;
        sumSignedDiffMs = 0.0;
        estimatedOffsetMs = 0.0;
        residualAbsMs = 0.0;
        resultMode = false;
        offsetApplied = false;
        tapTimes.clear();
        clearLanePressedStates();
    }

    private void showTapFeedback(Judgement judgement, double tapTime) {
        judgeAlpha = 0.85f;

        switch (judgement) {
            case PERFECT -> lastJudge = "Perfect";
            case GREAT -> lastJudge = "Great";
            case GOOD -> lastJudge = "Good";
            case EARLY -> lastJudge = "Early";
            case LATE -> lastJudge = "Late";
            case MISS -> lastJudge = "Miss";
            case NONE -> lastJudge = "Sample";
            default -> lastJudge = "";
        }

        double previewDiff = findNearestNoteDiffSeconds(tapTime);
        lastDiffMs = Double.isNaN(previewDiff) ? 0.0 : previewDiff * 1000.0;
    }

    private double findNearestNoteDiffSeconds(double tapTime) {
        if (chartHitTimes.isEmpty()) {
            return Double.NaN;
        }

        double bestDiff = Double.NaN;
        double bestAbs = Double.MAX_VALUE;

        for (double noteTime : chartHitTimes) {
            double diff = tapTime - noteTime;
            double abs = Math.abs(diff);

            if (abs < bestAbs) {
                bestAbs = abs;
                bestDiff = diff;
            }
        }

        return bestDiff;
    }

    private void applyOffsetOnce() {
        if (offsetApplied) {
            return;
        }

        offsetApplied = true;

        CorrectionEstimate estimate = estimateGlobalOffset();

        estimatedOffsetMs = estimate.offsetSeconds * 1000.0;
        residualAbsMs = estimate.meanAbsResidualSeconds * 1000.0;
        hitCount = estimate.matchCount;
        sumAbsDiffMs = residualAbsMs * hitCount;
        sumSignedDiffMs = estimatedOffsetMs * hitCount;

        context.setGlobalOffset(context.getGlobalOffset() - estimate.offsetSeconds);
    }

    private CorrectionEstimate estimateGlobalOffset() {
        if (tapTimes.isEmpty() || chartHitTimes.isEmpty()) {
            return new CorrectionEstimate(0.0, 0.0, 0);
        }

        CandidateScore best = null;

        for (double candidate = OFFSET_SEARCH_MIN_SECONDS;
             candidate <= OFFSET_SEARCH_MAX_SECONDS;
             candidate += OFFSET_SEARCH_COARSE_STEP_SECONDS) {

            CandidateScore score = scoreCandidateOffset(candidate);
            if (isBetterScore(score, best)) {
                best = score;
            }
        }

        if (best == null) {
            return new CorrectionEstimate(0.0, 0.0, 0);
        }

        double fineStart = Math.max(OFFSET_SEARCH_MIN_SECONDS,
                best.offsetSeconds - OFFSET_SEARCH_COARSE_STEP_SECONDS);
        double fineEnd = Math.min(OFFSET_SEARCH_MAX_SECONDS,
                best.offsetSeconds + OFFSET_SEARCH_COARSE_STEP_SECONDS);

        for (double candidate = fineStart;
             candidate <= fineEnd;
             candidate += OFFSET_SEARCH_FINE_STEP_SECONDS) {

            CandidateScore score = scoreCandidateOffset(candidate);
            if (isBetterScore(score, best)) {
                best = score;
            }
        }

        double refinedOffset = best.offsetSeconds + best.meanSignedResidualSeconds;
        refinedOffset = Math.max(OFFSET_SEARCH_MIN_SECONDS, Math.min(OFFSET_SEARCH_MAX_SECONDS, refinedOffset));

        CandidateScore refined = scoreCandidateOffset(refinedOffset);
        if (isBetterScore(refined, best)) {
            best = refined;
        }

        return new CorrectionEstimate(
                best.offsetSeconds,
                best.meanAbsResidualSeconds,
                best.matchCount
        );
    }

    private CandidateScore scoreCandidateOffset(double candidateOffsetSeconds) {
        int tapIndex = 0;
        int noteIndex = 0;
        int matchCount = 0;

        double sumAbsResidual = 0.0;
        double sumSignedResidual = 0.0;

        while (tapIndex < tapTimes.size() && noteIndex < chartHitTimes.size()) {
            double adjustedTap = tapTimes.get(tapIndex) - candidateOffsetSeconds;
            double diff = adjustedTap - chartHitTimes.get(noteIndex);

            if (Math.abs(diff) <= TAP_MATCH_TOLERANCE_SECONDS) {
                matchCount++;
                sumAbsResidual += Math.abs(diff);
                sumSignedResidual += diff;
                tapIndex++;
                noteIndex++;
            } else if (diff < -TAP_MATCH_TOLERANCE_SECONDS) {
                tapIndex++;
            } else {
                noteIndex++;
            }
        }

        if (matchCount == 0) {
            return new CandidateScore(candidateOffsetSeconds, 0, Double.MAX_VALUE, 0.0);
        }

        return new CandidateScore(
                candidateOffsetSeconds,
                matchCount,
                sumAbsResidual / matchCount,
                sumSignedResidual / matchCount
        );
    }

    private boolean isBetterScore(CandidateScore candidate, CandidateScore best) {
        if (candidate == null) {
            return false;
        }
        if (best == null) {
            return true;
        }
        if (candidate.matchCount != best.matchCount) {
            return candidate.matchCount > best.matchCount;
        }
        if (candidate.meanAbsResidualSeconds != best.meanAbsResidualSeconds) {
            return candidate.meanAbsResidualSeconds < best.meanAbsResidualSeconds;
        }
        return Math.abs(candidate.offsetSeconds) < Math.abs(best.offsetSeconds);
    }

    private void startScheduledMusicThread() {
        if (musicThreadStarted) {
            return;
        }

        musicThreadStarted = true;

        Thread t = new Thread(() -> {
            try {
                while (true) {
                    long playRequestNano = songStartNano - (long) (AUDIO_OUTPUT_LATENCY * 1_000_000_000L);
                    long now = System.nanoTime();
                    long remain = playRequestNano - now;

                    if (remain <= 0) {
                        break;
                    }

                    if (remain > 2_000_000L) {
                        Thread.sleep(1);
                    } else {
                        Thread.yield();
                    }
                }

                context.bgm.playLoaded(false);
                audioStarted = true;

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        t.setDaemon(true);
        t.start();
    }

    private void renderNotes(Graphics2D g, LaneLayout layout, double gameTime) {
        Map<Lane, List<Note>> notesByLane = nm.getLaneNotes();

        for (Lane lane : layout.getLanes()) {
            int x = layout.getLaneX(lane);
            int laneWidth = layout.getLaneWidth(lane);
            List<Note> notes = notesByLane.getOrDefault(lane, Collections.emptyList());

            for (Note note : notes) {
                drawNote(g, x, laneWidth, note, false, gameTime);
            }

            Note activeLongNote = nm.getActiveLongNote(lane);
            if (activeLongNote != null) {
                drawNote(g, x, laneWidth, activeLongNote, true, gameTime);
            }
        }
    }

    private void drawNote(Graphics2D g, int laneX, int laneWidth, Note note, boolean active, double gameTime) {
        if (note.isLongNote()) {
            drawLongNote(g, laneX, laneWidth, note, active, gameTime);
            return;
        }

        int y = (int) Math.round(JUDGEMENT_LINE_Y - (note.getHitTime() - gameTime) * NOTE_SCROLL_SPEED);
        drawNoteHead(g, laneX, laneWidth, y);
    }

    private void drawLongNote(Graphics2D g, int laneX, int laneWidth, Note note, boolean active, double gameTime) {
        double headTime = active ? Math.max(note.getHitTime(), gameTime) : note.getHitTime();
        int headY = (int) Math.round(JUDGEMENT_LINE_Y - (headTime - gameTime) * NOTE_SCROLL_SPEED);
        int tailY = (int) Math.round(JUDGEMENT_LINE_Y - (note.getEndTime() - gameTime) * NOTE_SCROLL_SPEED);

        int drawWidth = getNoteDrawWidth(laneWidth);
        int drawHeight = getNoteDrawHeight();
        int drawX = laneX + (laneWidth - drawWidth) / 2;

        int topY = Math.min(headY, tailY);
        int bottomY = Math.max(headY, tailY);
        int bodyX = drawX + Math.max(2, drawWidth / 6);
        int bodyWidth = Math.max(10, drawWidth - Math.max(4, drawWidth / 3));
        int bodyTop = topY + drawHeight / 2;
        int bodyHeight = Math.max(0, (bottomY + drawHeight / 2) - bodyTop);

        if (bodyHeight > 0) {
            g.setColor(new Color(120, 220, 255));
            g.fillRoundRect(bodyX, bodyTop, bodyWidth, bodyHeight, 12, 12);
        }

        drawNoteHead(g, laneX, laneWidth, tailY);
        drawNoteHead(g, laneX, laneWidth, headY);
    }

    private void drawNoteHead(Graphics2D g, int laneX, int laneWidth, int y) {
        int drawWidth = getNoteDrawWidth(laneWidth);
        int drawHeight = getNoteDrawHeight();
        int drawX = laneX + (laneWidth - drawWidth) / 2;

        if (noteImage != null && noteImage.getWidth(null) > 0 && noteImage.getHeight(null) > 0) {
            g.drawImage(noteImage, drawX, y, drawWidth, drawHeight, null);
            return;
        }

        g.setColor(Color.CYAN);
        g.fillRoundRect(drawX, y, drawWidth, drawHeight, 10, 10);
    }

    private int getNoteDrawWidth(int laneWidth) {
        if (noteImage != null && noteImage.getWidth(null) > 0) {
            return Math.min(noteImage.getWidth(null), Math.max(20, laneWidth - 8));
        }
        return Math.max(20, laneWidth - 20);
    }

    private int getNoteDrawHeight() {
        if (noteImage != null && noteImage.getHeight(null) > 0) {
            return noteImage.getHeight(null);
        }
        return 16;
    }

    private void drawJudgementLine(Graphics2D g, LaneLayout layout) {
        if (judgementLine != null) {
            int lineHeight = judgementLine.getHeight(null) > 0 ? judgementLine.getHeight(null) : 8;
            g.drawImage(judgementLine, layout.getStartX(), JUDGEMENT_LINE_Y, layout.getTotalWidth(), lineHeight, null);
        }
    }

    private void renderHud(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(judgeAlpha, 0.25f)));
            g2.setFont(new Font("SansSerif", Font.BOLD, 48));

            if ("Perfect".equals(lastJudge)) {
                g2.setColor(Color.BLUE);
            } else if ("Great".equals(lastJudge)) {
                g2.setColor(Color.CYAN);
            } else if ("Good".equals(lastJudge)) {
                g2.setColor(Color.GREEN);
            } else if ("Early".equals(lastJudge) || "Late".equals(lastJudge)) {
                g2.setColor(Color.ORANGE);
            } else if ("Miss".equals(lastJudge)) {
                g2.setColor(Color.GRAY);
            } else if ("Sample".equals(lastJudge)) {
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(Color.WHITE);
            }

            RenderUtils.drawCenteredString(g2, lastJudge, 30, 180, 320, 80);

            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            if (!lastJudge.isEmpty()) {
                RenderUtils.drawCenteredString(g2, String.format("%+.1f ms", lastDiffMs), 30, 240, 320, 70);
            }
        } finally {
            g2.dispose();
        }
    }

    private void drawLaneKeyBindings(Graphics2D g, LaneLayout layout) {
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            int textY = 405;
            int textHeight = 42;
            int fontSize = Math.max(16, Math.min(28, layout.getMinLaneWidth() - 18));
            g2.setFont(new Font("Arial", Font.BOLD, fontSize));

            for (Lane lane : layout.getLanes()) {
                int laneX = layout.getLaneX(lane);
                int laneWidth = layout.getLaneWidth(lane);
                String keyText = context.getKeyTextForLane(lane);

                RenderUtils.drawOutlinedCenteredString(
                        g2,
                        keyText,
                        laneX,
                        textY,
                        laneWidth,
                        textHeight,
                        Color.WHITE,
                        new Color(0, 0, 0, 180)
                );
            }
        } finally {
            g2.dispose();
        }
    }

    private void initializeLanePressedMap() {
        lanePressed.clear();
        for (Lane lane : Lane.values()) {
            lanePressed.put(lane, false);
        }
    }

    private boolean isLanePressed(Lane lane) {
        return lanePressed.getOrDefault(lane, false);
    }

    private void setLanePressed(Lane lane, boolean pressed) {
        lanePressed.put(lane, pressed);
    }

    private void clearLanePressedStates() {
        for (Lane lane : Lane.values()) {
            lanePressed.put(lane, false);
        }
    }

    private void renderResult(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 44));
            RenderUtils.drawCenteredString(g2, "RESULT", 0, 120, SCREEN_WIDTH, 70);

            double avgAbs = hitCount > 0 ? (sumAbsDiffMs / hitCount) : 0.0;
            double avgSigned = hitCount > 0 ? (sumSignedDiffMs / hitCount) : 0.0;

            g2.setFont(new Font("SansSerif", Font.PLAIN, 26));
            RenderUtils.drawCenteredString(g2, "Matched Samples: " + hitCount, 0, 205, SCREEN_WIDTH, 40);
            RenderUtils.drawCenteredString(g2, String.format("Estimated Offset: %+.2f ms", estimatedOffsetMs), 0, 250, SCREEN_WIDTH, 40);
            RenderUtils.drawCenteredString(g2, String.format("Residual Error (abs): %.2f ms", residualAbsMs), 0, 295, SCREEN_WIDTH, 40);
            RenderUtils.drawCenteredString(g2, String.format("Avg Error (signed): %+.2f ms", avgSigned), 0, 340, SCREEN_WIDTH, 40);
            RenderUtils.drawCenteredString(g2, String.format("Global Offset: %+.4f s", context.getGlobalOffset()), 0, 385, SCREEN_WIDTH, 40);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
            RenderUtils.drawCenteredString(g2, "ENTER: Retry ESC: Back", 0, 440, SCREEN_WIDTH, 35);
        } finally {
            g2.dispose();
        }
    }

    private void drawLane(Graphics2D g, LaneLayout layout) {
        java.awt.Composite original = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

        for (Lane lane : layout.getLanes()) {
            int x = layout.getLaneX(lane);
            int laneWidth = layout.getLaneWidth(lane);

            g.setColor(new Color(13, 13, 13));
            g.fillRect(x, 0, laneWidth, layout.getLaneHeight());

            g.setColor(new Color(21, 21, 21));
            g.fillRect(x + 5, 0, Math.max(0, laneWidth - 10), layout.getLaneHeight());

            g.setColor(new Color(42, 42, 42));
            g.fillRect(x, 0, 2, layout.getLaneHeight());
            g.fillRect(x + laneWidth - 2, 0, 2, layout.getLaneHeight());
        }

        g.setComposite(original);
    }

    private static class CandidateScore {
        final double offsetSeconds;
        final int matchCount;
        final double meanAbsResidualSeconds;
        final double meanSignedResidualSeconds;

        CandidateScore(double offsetSeconds, int matchCount,
                       double meanAbsResidualSeconds, double meanSignedResidualSeconds) {
            this.offsetSeconds = offsetSeconds;
            this.matchCount = matchCount;
            this.meanAbsResidualSeconds = meanAbsResidualSeconds;
            this.meanSignedResidualSeconds = meanSignedResidualSeconds;
        }
    }

    private static class CorrectionEstimate {
        final double offsetSeconds;
        final double meanAbsResidualSeconds;
        final int matchCount;

        CorrectionEstimate(double offsetSeconds, double meanAbsResidualSeconds, int matchCount) {
            this.offsetSeconds = offsetSeconds;
            this.meanAbsResidualSeconds = meanAbsResidualSeconds;
            this.matchCount = matchCount;
        }
    }
}
****************************/