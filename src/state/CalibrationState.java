package state;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import stage.CorrectionConfig;
import state.gameplay.Lane;
import util.RenderUtils;

public class CalibrationState implements GameState {

    private static final int SCREEN_WIDTH = 1024;
    private static final int SCREEN_HEIGHT = 576;

    private static final double LEAD_IN = 3.0;
    private static final double AUDIO_OUTPUT_LATENCY = 0.2;

    private static final double DEFAULT_ROTATION_BEATS = 4.0;
    private static final double FINE_OFFSET_STEP_SECONDS = 0.001;
    private static final double COARSE_OFFSET_STEP_SECONDS = 0.010;

    private static final int ORBIT_RADIUS = 170;
    private static final int NOTE_DRAW_SIZE = 42;

    private static final double HIT_MARKER_LIFETIME = 0.35;
    private static final int HIT_MARKER_BASE_RADIUS = 18;
    private static final int HIT_MARKER_EXPAND_RADIUS = 16;

    private final GameContext context;
    private final CorrectionConfig correctionConfig;
    private final AssetManager am = AssetManager.getInstance();

    private Image background;
    private Image noteImage;

    private boolean preloaded = false;
    private volatile boolean musicThreadStarted = false;
    private volatile boolean audioStarted = false;
    private boolean paused = false;

    private volatile long songStartNano = 0L;
    private long pauseStartNano = 0L;

    private double timelineTime = -LEAD_IN;

    private final double bpm;
    private final double musicOffsetSeconds;
    private final double rotationBeats;

    private final EnumMap<Lane, Boolean> lanePressed = new EnumMap<>(Lane.class);
    private final List<OrbitHitMarker> hitMarkers = new ArrayList<>();

    public CalibrationState(GameContext context, CorrectionConfig correctionConfig) {
        this(context, correctionConfig, DEFAULT_ROTATION_BEATS);
    }

    public CalibrationState(GameContext context, CorrectionConfig correctionConfig, double rotationBeats) {
        this.context = context;
        this.correctionConfig = correctionConfig;
        this.bpm = correctionConfig.getMusicBPM();
        this.musicOffsetSeconds = correctionConfig.getMusicOffset();
        this.rotationBeats = rotationBeats <= 0.0 ? DEFAULT_ROTATION_BEATS : rotationBeats;
        initializeLanePressedMap();
    }

    @Override
    public void enter() {
        context.bgm.stop();

        if (!preloaded) {
            preload();
        } else {
            context.bgm.load(correctionConfig.getMusicPath());
        }

        clearLanePressedStates();
        hitMarkers.clear();

        paused = false;
        audioStarted = false;
        musicThreadStarted = false;
        pauseStartNano = 0L;
        timelineTime = -LEAD_IN;

        long now = System.nanoTime();
        songStartNano = now + (long) (LEAD_IN * 1_000_000_000L);

        startScheduledMusicThread();
    }

    @Override
    public void exit() {
        context.bgm.stop();
    }

    @Override
    public void update(double deltaTime) {
        if (!paused) {
            updateTimelineTime();
        }

        updateHitMarkers(deltaTime);

        if (!paused && audioStarted && !context.bgm.isPlaying() && timelineTime > 0) {
            restartPlayback();
        }
    }

    @Override
    public void render(Graphics2D g) {
        if (!paused) {
            updateTimelineTime();
        }

        drawBackground(g);
        drawOrbitScene(g);
        drawHud(g);

        if (paused) {
            drawPauseOverlay(g);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_ESCAPE) {
            context.changeState(new LevelSelectState(context));
            return;
        }

        if (keyCode == KeyEvent.VK_SPACE) {
            if (paused) {
                resumePlayback();
            } else {
                pausePlayback();
            }
            return;
        }

        if (keyCode == KeyEvent.VK_R) {
            restartPlayback();
            return;
        }

        if (keyCode == KeyEvent.VK_ENTER) {
            context.changeState(new LevelSelectState(context));
            return;
        }

        Lane inputLane = context.getLaneForKeyCode(keyCode);
        if (inputLane != null) {
            if (isLanePressed(inputLane)) {
                return;
            }

            setLanePressed(inputLane, true);
            addHitMarkerAtCurrentOrbitPosition(inputLane);
            return;
        }

        if (paused) {
            return;
        }

        boolean shift = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;
        double step = shift ? COARSE_OFFSET_STEP_SECONDS : FINE_OFFSET_STEP_SECONDS;

        if (keyCode == KeyEvent.VK_LEFT) {
            context.setGlobalOffset(context.getGlobalOffset() - step);
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            context.setGlobalOffset(context.getGlobalOffset() + step);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Lane inputLane = context.getLaneForKeyCode(e.getKeyCode());
        if (inputLane != null) {
            setLanePressed(inputLane, false);
        }
    }

    void preload() {
        background = am.getImage(correctionConfig.getBackgroundImageKey());
        noteImage = am.getImage("note_" + context.getNoteIndex());
        context.bgm.load(correctionConfig.getMusicPath());
        preloaded = true;
    }

    private void updateTimelineTime() {
        timelineTime = getScheduledPlaybackTime();
    }

    private double getScheduledPlaybackTime() {
        long now = System.nanoTime();
        return ((now - songStartNano) / 1_000_000_000.0) + AUDIO_OUTPUT_LATENCY;
    }

    private void startScheduledMusicThread() {
        if (musicThreadStarted) {
            return;
        }

        musicThreadStarted = true;

        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    long playRequestNano = songStartNano - (long) (AUDIO_OUTPUT_LATENCY * 1_000_000_000L);
                    long now = System.nanoTime();
                    long remain = playRequestNano - now;

                    if (remain <= 0) {
                        break;
                    }

                    if (paused) {
                        Thread.sleep(1);
                        continue;
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

        thread.setDaemon(true);
        thread.start();
    }

    private void restartPlayback() {
        context.bgm.stop();
        context.bgm.load(correctionConfig.getMusicPath());

        clearLanePressedStates();
        hitMarkers.clear();

        paused = false;
        audioStarted = false;
        musicThreadStarted = false;
        pauseStartNano = 0L;
        timelineTime = -LEAD_IN;

        long now = System.nanoTime();
        songStartNano = now + (long) (LEAD_IN * 1_000_000_000L);

        startScheduledMusicThread();
    }

    private void pausePlayback() {
        paused = true;
        pauseStartNano = System.nanoTime();

        if (context.bgm.isPlaying()) {
            context.bgm.pause();
        }
    }

    private void resumePlayback() {
        long now = System.nanoTime();
        long pausedDuration = now - pauseStartNano;

        songStartNano += pausedDuration;
        paused = false;

        if (context.bgm.isPaused()) {
            context.bgm.resume();
        }
    }

    private void drawBackground(Graphics2D g) {
        if (background != null) {
            g.drawImage(background, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, null);
            return;
        }

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    private void drawOrbitScene(Graphics2D g) {
        int centerX = 350;
        int centerY = 288;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(70, 70, 560, 430, 30, 30);

            drawOrbit(g2, centerX, centerY);
            drawBeatMarkers(g2, centerX, centerY);
            drawHitMarker(g2, centerX, centerY);
            drawOrbitHitMarkers(g2, centerX, centerY);
            drawRotatingNote(g2, centerX, centerY);
        } finally {
            g2.dispose();
        }
    }

    private void drawOrbit(Graphics2D g, int centerX, int centerY) {
        g.setStroke(new BasicStroke(4f));
        g.setColor(new Color(180, 180, 180));
        g.drawOval(centerX - ORBIT_RADIUS, centerY - ORBIT_RADIUS, ORBIT_RADIUS * 2, ORBIT_RADIUS * 2);

        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(120, 120, 120));
        g.drawLine(centerX - ORBIT_RADIUS - 20, centerY, centerX + ORBIT_RADIUS + 20, centerY);
        g.drawLine(centerX, centerY - ORBIT_RADIUS - 20, centerX, centerY + ORBIT_RADIUS + 20);
    }

    private void drawBeatMarkers(Graphics2D g, int centerX, int centerY) {
        Color[] colors = {
                new Color(255, 80, 80),
                new Color(80, 180, 255),
                new Color(110, 255, 140),
                new Color(255, 220, 70)
        };

        for (int i = 0; i < 4; i++) {
            double angle = -Math.PI / 2 + (Math.PI / 2.0) * i;
            int x = centerX + (int) Math.round(Math.cos(angle) * ORBIT_RADIUS);
            int y = centerY + (int) Math.round(Math.sin(angle) * ORBIT_RADIUS);

            g.setColor(colors[i]);
            g.fillOval(x - 9, y - 9, 18, 18);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString(String.valueOf(i + 1), x - 4, y - 16);
        }
    }

    private void drawHitMarker(Graphics2D g, int centerX, int centerY) {
        int x = centerX;
        int y = centerY - ORBIT_RADIUS;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(x, y - 28, x, y + 28);
            g2.drawLine(x - 18, y, x + 18, y);

            g2.setFont(new Font("Arial", Font.BOLD, 18));
            RenderUtils.drawCenteredString(g2, "HIT", centerX - 40, centerY - ORBIT_RADIUS - 62, 80, 24);
        } finally {
            g2.dispose();
        }
    }

    private void drawRotatingNote(Graphics2D g, int centerX, int centerY) {
        double angle = getRotationAngle();
        int x = centerX + (int) Math.round(Math.cos(angle) * ORBIT_RADIUS);
        int y = centerY + (int) Math.round(Math.sin(angle) * ORBIT_RADIUS);

        if (noteImage != null && noteImage.getWidth(null) > 0 && noteImage.getHeight(null) > 0) {
            g.drawImage(noteImage, x - NOTE_DRAW_SIZE / 2, y - NOTE_DRAW_SIZE / 2, NOTE_DRAW_SIZE, NOTE_DRAW_SIZE, null);
            return;
        }

        g.setColor(Color.CYAN);
        g.fillOval(x - NOTE_DRAW_SIZE / 2, y - NOTE_DRAW_SIZE / 2, NOTE_DRAW_SIZE, NOTE_DRAW_SIZE);
        g.setColor(Color.WHITE);
        g.drawOval(x - NOTE_DRAW_SIZE / 2, y - NOTE_DRAW_SIZE / 2, NOTE_DRAW_SIZE, NOTE_DRAW_SIZE);
    }

    private void drawOrbitHitMarkers(Graphics2D g, int centerX, int centerY) {
        if (hitMarkers.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            for (OrbitHitMarker marker : hitMarkers) {
                float progress = (float) (1.0 - (marker.life / marker.maxLife));
                float alpha = (float) Math.max(0.0, marker.life / marker.maxLife);

                int radius = HIT_MARKER_BASE_RADIUS + Math.round(HIT_MARKER_EXPAND_RADIUS * progress);

                int x = centerX + (int) Math.round(Math.cos(marker.angle) * ORBIT_RADIUS);
                int y = centerY + (int) Math.round(Math.sin(marker.angle) * ORBIT_RADIUS);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.90f));
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(x - radius, y - radius, radius * 2, radius * 2);

                int innerRadius = Math.max(6, radius / 3);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.35f));
                g2.setColor(getLaneMarkerColor(marker.lane));
                g2.fillOval(x - innerRadius, y - innerRadius, innerRadius * 2, innerRadius * 2);
            }
        } finally {
            g2.dispose();
        }
    }

    private Color getLaneMarkerColor(Lane lane) {
        if (lane == null) {
            return new Color(120, 220, 255);
        }

        return switch (lane.ordinal() % 4) {
            case 0 -> new Color(255, 120, 120);
            case 1 -> new Color(120, 180, 255);
            case 2 -> new Color(120, 255, 170);
            default -> new Color(255, 220, 120);
        };
    }

    private void addHitMarkerAtCurrentOrbitPosition(Lane lane) {
        hitMarkers.add(new OrbitHitMarker(lane, getRotationAngle(), HIT_MARKER_LIFETIME));
    }

    private void updateHitMarkers(double deltaTime) {
        for (int i = hitMarkers.size() - 1; i >= 0; i--) {
            OrbitHitMarker marker = hitMarkers.get(i);
            marker.life -= deltaTime;

            if (marker.life <= 0.0) {
                hitMarkers.remove(i);
            }
        }
    }

    private double getRotationAngle() {
        double secondsPerBeat = 60.0 / bpm;

        double correctedTimeline = timelineTime + context.getGlobalOffset();
        double beatPosition = (correctedTimeline - musicOffsetSeconds) / secondsPerBeat;

        double normalizedPhase = beatPosition % rotationBeats;
        if (normalizedPhase < 0.0) {
            normalizedPhase += rotationBeats;
        }

        return -Math.PI / 2 + (normalizedPhase / rotationBeats) * (Math.PI * 2.0);
    }

    private void drawHud(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f));
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRoundRect(660, 70, 300, 430, 30, 30);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 34));
            RenderUtils.drawCenteredString(g2, "OFFSET CALIBRATION", 660, 95, 300, 38);

            g2.setFont(new Font("Arial", Font.PLAIN, 22));
            g2.drawString(String.format("BPM: %.3f", bpm), 690, 165);
            g2.drawString(String.format("Song Offset: %+.3f s", musicOffsetSeconds), 690, 200);
            g2.drawString(String.format("Global Offset: %+.1f ms", context.getGlobalOffset() * 1000.0), 690, 235);

            g2.drawString(String.format("Rotation: %.1f beats / turn", rotationBeats), 690, 270);

            double beatNow = getCurrentBeatPosition();
            g2.drawString(String.format("Beat Position: %.3f", beatNow), 690, 305);

            String playbackText;
            if (timelineTime < 0.0) {
                playbackText = String.format("Starting in %.2f s", -timelineTime);
            } else if (paused) {
                playbackText = "Paused";
            } else {
                playbackText = String.format("Playback: %.2f s", timelineTime);
            }
            g2.drawString(playbackText, 690, 340);

            g2.setFont(new Font("Arial", Font.BOLD, 22));
            g2.setColor(new Color(120, 220, 255));
            g2.drawString("Controls", 690, 395);

            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            g2.setColor(Color.WHITE);
            g2.drawString("Lane Key : Leave hit circle", 690, 430);
            g2.drawString("Left / Right : -1ms / +1ms", 690, 455);
            g2.drawString("Shift + Arrow : -10ms / +10ms", 690, 480);
            g2.drawString("Space : Pause / Resume", 690, 505);
            g2.drawString("R : Restart", 690, 530);
            g2.drawString("Enter or ESC : Exit", 690, 555);

        } finally {
            g2.dispose();
        }
    }

    private double getCurrentBeatPosition() {
        double secondsPerBeat = 60.0 / bpm;
        double correctedTimeline = timelineTime + context.getGlobalOffset();
        return (correctedTimeline - musicOffsetSeconds) / secondsPerBeat;
    }

    private void drawPauseOverlay(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 46));
            RenderUtils.drawCenteredString(g2, "PAUSED", 0, 215, SCREEN_WIDTH, 50);

            g2.setFont(new Font("Arial", Font.PLAIN, 24));
            RenderUtils.drawCenteredString(g2, "Press SPACE to resume", 0, 280, SCREEN_WIDTH, 30);
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

    private static class OrbitHitMarker {
        private final Lane lane;
        private final double angle;
        private final double maxLife;
        private double life;

        private OrbitHitMarker(Lane lane, double angle, double life) {
            this.lane = lane;
            this.angle = angle;
            this.life = life;
            this.maxLife = life;
        }
    }
}