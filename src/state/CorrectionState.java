package state;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
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

    public CorrectionState(GameContext context, CorrectionConfig correctionConfig) {
        this.context = context;
        this.correctionConfig = correctionConfig;
    }

    @Override
    public void enter() {
        resetRunState();
        context.bgm.stop();

        if (!preloaded) {
            preload();
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
            int missCount = nm.update(gameTime);
            for (int i = 0; i < missCount; i++) {
                applyJudgement(Judgement.MISS);
            }

            if (audioStarted && !context.bgm.isPlaying() && timelineTime > 0 && nm.isFinished()) {
                resultMode = true;
                applyOffsetOnce();
            }
        }

        judgeAlpha = Math.max(0.0f, judgeAlpha - 0.02f);
    }

    @Override
    public void render(Graphics2D g) {
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
            result = nm.judge(inputLane, judgeTime);
        }

        applyJudgement(result);
    }

    @Override
    public void keyReleased(KeyEvent e) {
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

        context.bgm.load(correctionConfig.getMusicPath());
        preloaded = true;
    }

    private LaneLayout getLaneLayout() {
        return new LaneLayout(context.getPlayableLanes(), PLAYFIELD_START_X, PLAYFIELD_WIDTH, SCREEN_HEIGHT);
    }

    private void updateTimelineTime() {
        double scheduledTime = getScheduledPlaybackTime();

        if (!audioStarted) {
            timelineTime = scheduledTime;
            return;
        }

        double clipTime = context.bgm.getPositionSeconds();
        timelineTime = Math.max(scheduledTime, clipTime);
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
        resultMode = false;
        offsetApplied = false;
    }

    private void applyOffsetOnce() {
        if (offsetApplied) {
            return;
        }

        offsetApplied = true;

        double avgSignedMs = hitCount > 0 ? (sumSignedDiffMs / hitCount) : 0.0;
        context.setGlobalOffset(context.getGlobalOffset() - (avgSignedMs / 1000.0));
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
            List<Note> notes = notesByLane.get(lane);

            if (notes == null) {
                continue;
            }

            for (Note note : notes) {
                double y = JUDGEMENT_LINE_Y - (note.getHitTime() - gameTime) * NOTE_SCROLL_SPEED;
                drawNote(g, x, laneWidth, (int) y);
            }
        }
    }

    private void drawNote(Graphics2D g, int laneX, int laneWidth, int y) {
        if (noteImage != null && noteImage.getWidth(null) > 0 && noteImage.getHeight(null) > 0) {
            int naturalWidth = noteImage.getWidth(null);
            int naturalHeight = noteImage.getHeight(null);
            int drawWidth = Math.min(naturalWidth, Math.max(20, laneWidth - 8));
            int drawHeight = naturalHeight;
            int drawX = laneX + (laneWidth - drawWidth) / 2;
            g.drawImage(noteImage, drawX, y, drawWidth, drawHeight, null);
            return;
        }

        int fallbackWidth = Math.max(20, laneWidth - 20);
        int drawX = laneX + (laneWidth - fallbackWidth) / 2;
        g.setColor(Color.CYAN);
        g.fillRect(drawX, y, fallbackWidth, 16);
    }

    private void drawJudgementLine(Graphics2D g, LaneLayout layout) {
        if (judgementLine != null) {
            int lineHeight = judgementLine.getHeight(null) > 0 ? judgementLine.getHeight(null) : 8;
            g.drawImage(judgementLine, layout.getStartX(), JUDGEMENT_LINE_Y, layout.getTotalWidth(), lineHeight, null);
        }
    }

    private void applyJudgement(Judgement j) {
        if (j == Judgement.NONE) {
            return;
        }

        judgeAlpha = 0.85f;

        switch (j) {
            case PERFECT -> lastJudge = "Perfect";
            case GREAT -> lastJudge = "Great";
            case GOOD -> lastJudge = "Good";
            case EARLY -> lastJudge = "Early";
            case LATE -> lastJudge = "Late";
            case MISS -> lastJudge = "Miss";
            default -> lastJudge = "";
        }

        if (j != Judgement.MISS) {
            lastDiffMs = nm.getLastHitTimeDiffSeconds() * 1000.0;
            hitCount++;
            sumAbsDiffMs += Math.abs(lastDiffMs);
            sumSignedDiffMs += lastDiffMs;
        } else {
            lastDiffMs = 0.0;
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
            } else {
                g2.setColor(Color.WHITE);
            }

            RenderUtils.drawCenteredString(g2, lastJudge, 30, 180, 320, 80);

            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            if (!lastJudge.isEmpty() && !"Miss".equals(lastJudge)) {
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
            RenderUtils.drawCenteredString(g2, "Hits: " + hitCount, 0, 215, SCREEN_WIDTH, 40);
            RenderUtils.drawCenteredString(g2, String.format("Avg Error (abs): %.2f ms", avgAbs), 0, 265, SCREEN_WIDTH, 40);
            RenderUtils.drawCenteredString(g2, String.format("Avg Error (signed): %+.2f ms", avgSigned), 0, 305, SCREEN_WIDTH, 40);
            RenderUtils.drawCenteredString(g2, String.format("Global Offset: %+.4f s", context.getGlobalOffset()), 0, 355, SCREEN_WIDTH, 40);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
            RenderUtils.drawCenteredString(g2, "ENTER: Retry ESC: Back", 0, 420, SCREEN_WIDTH, 35);
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
}
