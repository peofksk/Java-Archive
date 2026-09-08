package state;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import stage.CalibrationConfig;
import state.gameplay.Lane;
import util.RenderUtils;

public class CalibrationState implements GameState {

    private static final int SCREEN_WIDTH = 1024;
    private static final int SCREEN_HEIGHT = 576;

    private static final double LEAD_IN = 3.0;

    private static final double DEFAULT_ROTATION_BEATS = 4.0;
    private static final double FINE_OFFSET_STEP_SECONDS = 0.001;
    private static final double COARSE_OFFSET_STEP_SECONDS = 0.010;

    private static final int ORBIT_RADIUS = 170;
    private static final int NOTE_DRAW_SIZE = 42;

    private static final double HIT_MARKER_SHRINK_DURATION = 0.22;
    private static final int HIT_MARKER_START_RADIUS = 34;
    private static final int HIT_MARKER_FINAL_RADIUS = 13;

    private static final int HUD_X = 660;
    private static final int HUD_Y = 70;
    private static final int HUD_W = 300;
    private static final int HUD_H = 465;

    private static final int HUD_PAD_X = 28;

    private static final int BACK_BUTTON_W = 76;
    private static final int BACK_BUTTON_H = 30;
    private static final int BACK_BUTTON_X = SCREEN_WIDTH - BACK_BUTTON_W - 22;
    private static final int BACK_BUTTON_Y = HUD_Y - BACK_BUTTON_H - 23;

    private final GameContext context;
    private final CalibrationConfig calibrationConfig;
    private final AssetManager am = AssetManager.getInstance();

    private Image background;
    private Image noteImage;

    private boolean preloaded = false;
    private volatile boolean audioStarted = false;

    private boolean paused = false;

    private boolean backButtonHovered = false;
    private boolean backButtonPressed = false;

    private double leadInRemainingSeconds = LEAD_IN;
    private double timelineTime = -LEAD_IN;

    private final double bpm;
    private final double musicOffsetSeconds;
    private final double rotationBeats;

    private final Map<Lane, Boolean> lanePressed = new HashMap<>();
    private final List<OrbitHitMarker> hitMarkers = new ArrayList<>();

    public CalibrationState(GameContext context, CalibrationConfig calibrationConfig) {
        this(context, calibrationConfig, DEFAULT_ROTATION_BEATS);
    }

    public CalibrationState(GameContext context, CalibrationConfig calibrationConfig, double rotationBeats) {
        this.context = context;
        this.calibrationConfig = calibrationConfig;
        this.bpm = calibrationConfig.getMusicBPM();
        this.musicOffsetSeconds = calibrationConfig.getMusicOffset();
        this.rotationBeats = rotationBeats <= 0.0 ? DEFAULT_ROTATION_BEATS : rotationBeats;

        initializeLanePressedMap();
    }

    @Override
    public void enter() {
        context.bgm.stop();

        if (!preloaded) {
            preload();
        } else {
            context.bgm.load(calibrationConfig.getMusicPath());
        }

        clearLanePressedStates();
        hitMarkers.clear();

        paused = false;
        audioStarted = false;
        leadInRemainingSeconds = LEAD_IN;
        timelineTime = -LEAD_IN;

        backButtonHovered = false;
        backButtonPressed = false;
    }

    @Override
    public void exit() {
        context.saveSettings();
        context.bgm.stop();

        paused = false;
        audioStarted = false;
        clearLanePressedStates();

        backButtonHovered = false;
        backButtonPressed = false;
    }

    @Override
    public void update(double deltaTime) {
        if (paused) {
            return;
        }

        advanceTimelineTime(deltaTime);
        updateHitMarkers(deltaTime);
    }

    @Override
    public void render(Graphics2D g) {
        updateTimelineTime();

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

        if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER) {
            backToLevelSelect();
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

        if (keyCode == KeyEvent.VK_BACK_SPACE) {
            removeLastHitMarker();
            return;
        }

        if (keyCode == KeyEvent.VK_DELETE) {
            clearHitMarkers();
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

    @Override
    public void mouseMoved(MouseEvent e) {
        backButtonHovered = getBackButtonBounds().contains(e.getPoint());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        backButtonPressed = getBackButtonBounds().contains(e.getPoint());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            backButtonPressed = false;
            return;
        }

        Point point = e.getPoint();

        if (backButtonPressed && getBackButtonBounds().contains(point)) {
            backButtonPressed = false;
            backToLevelSelect();
            return;
        }

        backButtonPressed = false;
    }

    void preload() {
        background = am.getImage(calibrationConfig.getBackgroundImageKey());
        noteImage = am.getImage("note_" + context.getNoteIndex());
        context.bgm.load(calibrationConfig.getMusicPath());
        preloaded = true;
    }

    private void advanceTimelineTime(double deltaTime) {
        if (audioStarted) {
            updateTimelineTime();
            return;
        }

        leadInRemainingSeconds -= deltaTime;

        if (leadInRemainingSeconds <= 0.0) {
            context.bgm.playLoaded(false);
            audioStarted = true;
            timelineTime = context.bgm.getPositionSeconds();
            return;
        }

        timelineTime = -leadInRemainingSeconds;
    }

    private void updateTimelineTime() {
        if (paused) {
            return;
        }

        if (audioStarted) {
            timelineTime = context.bgm.getPositionSeconds();
            return;
        }

        timelineTime = -leadInRemainingSeconds;
    }

    private void restartPlayback() {
        context.bgm.stop();
        context.bgm.load(calibrationConfig.getMusicPath());

        clearLanePressedStates();
        hitMarkers.clear();

        paused = false;
        audioStarted = false;
        leadInRemainingSeconds = LEAD_IN;
        timelineTime = -LEAD_IN;
    }

    private void pausePlayback() {
        if (paused) {
            return;
        }

        updateTimelineTime();

        paused = true;
        clearLanePressedStates();

        if (audioStarted && context.bgm.isPlaying()) {
            context.bgm.pause();
        }
    }

    private void resumePlayback() {
        if (!paused) {
            return;
        }

        paused = false;
        clearLanePressedStates();

        if (audioStarted && context.bgm.isPaused()) {
            context.bgm.resume();
        }

        updateTimelineTime();
    }

    private void backToLevelSelect() {
        context.changeState(new LevelSelectState(context));
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
                double progress = marker.age / marker.shrinkDuration;
                progress = Math.max(0.0, Math.min(1.0, progress));

                int radius = (int) Math.round(
                        HIT_MARKER_START_RADIUS
                                + (HIT_MARKER_FINAL_RADIUS - HIT_MARKER_START_RADIUS) * progress
                );

                int x = centerX + (int) Math.round(Math.cos(marker.angle) * ORBIT_RADIUS);
                int y = centerY + (int) Math.round(Math.sin(marker.angle) * ORBIT_RADIUS);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f));
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(x - radius, y - radius, radius * 2, radius * 2);

                int innerRadius = Math.max(5, radius / 3);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
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

        int laneIndex = context.getLaneIndex(lane);

        return switch (Math.floorMod(laneIndex, 4)) {
            case 0 -> new Color(255, 120, 120);
            case 1 -> new Color(120, 180, 255);
            case 2 -> new Color(120, 255, 170);
            default -> new Color(255, 220, 120);
        };
    }

    private void addHitMarkerAtCurrentOrbitPosition(Lane lane) {
        hitMarkers.add(new OrbitHitMarker(lane, getRotationAngle(), HIT_MARKER_SHRINK_DURATION));
    }

    private void removeLastHitMarker() {
        if (hitMarkers.isEmpty()) {
            return;
        }

        hitMarkers.remove(hitMarkers.size() - 1);
    }

    private void clearHitMarkers() {
        hitMarkers.clear();
    }

    private void updateHitMarkers(double deltaTime) {
        for (OrbitHitMarker marker : hitMarkers) {
            marker.age += deltaTime;

            if (marker.age > marker.shrinkDuration) {
                marker.age = marker.shrinkDuration;
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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f));
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRoundRect(HUD_X, HUD_Y, HUD_W, HUD_H, 30, 30);

            drawBackButton(g2);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            int textX = HUD_X + HUD_PAD_X;
            int contentW = HUD_W - HUD_PAD_X * 2;

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 27));
            g2.drawString("CALIBRATION", textX, HUD_Y + 50);

            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            g2.drawString(String.format("BPM: %.3f", bpm), textX, HUD_Y + 86);
            g2.drawString(String.format("Offset: %+.1f ms", context.getGlobalOffset() * 1000.0), textX, HUD_Y + 114);

            double beatNow = getCurrentBeatPosition();
            g2.drawString(String.format("Beat: %.3f", beatNow), textX, HUD_Y + 142);

            String playbackText;
            if (timelineTime < 0.0) {
                playbackText = String.format("Start in %.2f s", -timelineTime);
            } else if (paused) {
                playbackText = "Paused";
            } else {
                playbackText = String.format("Time: %.2f s", timelineTime);
            }
            g2.drawString(playbackText, textX, HUD_Y + 170);

            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(new Color(120, 220, 255));
            g2.drawString("How to use", textX, HUD_Y + 215);

            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.setColor(Color.WHITE);

            drawHudLine(g2, "1. Listen to the beat.", textX, HUD_Y + 243);
            drawHudLine(g2, "2. Press any lane key on beat.", textX, HUD_Y + 266);
            drawHudLine(g2, "3. Check where markers appear.", textX, HUD_Y + 289);
            drawHudLine(g2, "4. Adjust until markers hit HIT.", textX, HUD_Y + 312);

            g2.setColor(new Color(255, 230, 130));
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            RenderUtils.drawCenteredString(
                    g2,
                    "Goal: markers should land near HIT",
                    textX,
                    HUD_Y + 323,
                    contentW,
                    20
            );

            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(new Color(120, 220, 255));
            g2.drawString("Controls", textX, HUD_Y + 355);

            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.setColor(Color.WHITE);

            drawHudLine(g2, "Lane Key : place marker", textX, HUD_Y + 382);
            drawHudLine(g2, "← / → : Offset ±1ms", textX, HUD_Y + 405);
            drawHudLine(g2, "Shift + ← / → : ±10ms", textX, HUD_Y + 428);
            drawHudLine(g2, "Space : pause    R : restart", textX, HUD_Y + 451);

        } finally {
            g2.dispose();
        }
    }

    private void drawHudLine(Graphics2D g, String text, int x, int baselineY) {
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(text, x + 1, baselineY + 1);

        g.setColor(Color.WHITE);
        g.drawString(text, x, baselineY);
    }

    private void drawBackButton(Graphics2D g) {
        Rectangle bounds = getBackButtonBounds();

        Color fill;
        Color border;
        Color text;

        if (backButtonPressed) {
            fill = new Color(40, 115, 170, 235);
            border = new Color(210, 245, 255, 255);
            text = Color.WHITE;
        } else if (backButtonHovered) {
            fill = new Color(70, 145, 205, 220);
            border = new Color(190, 235, 255, 245);
            text = Color.WHITE;
        } else {
            fill = new Color(20, 25, 35, 220);
            border = new Color(130, 190, 230, 210);
            text = new Color(235, 248, 255);
        }

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        g.setColor(new Color(0, 0, 0, 130));
        g.fillRoundRect(bounds.x + 2, bounds.y + 3, bounds.width, bounds.height, 12, 12);

        g.setColor(fill);
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);

        g.setStroke(new BasicStroke(2f));
        g.setColor(border);
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);

        g.setFont(new Font("Arial", Font.BOLD, 14));

        g.setColor(new Color(0, 0, 0, 130));
        RenderUtils.drawCenteredString(g, "BACK", bounds.x + 1, bounds.y + 6, bounds.width, 18);

        g.setColor(text);
        RenderUtils.drawCenteredString(g, "BACK", bounds.x, bounds.y + 5, bounds.width, 18);
    }

    private Rectangle getBackButtonBounds() {
        return new Rectangle(BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_W, BACK_BUTTON_H);
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

        for (Lane lane : context.getPlayableLanes()) {
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
        lanePressed.clear();

        for (Lane lane : context.getPlayableLanes()) {
            lanePressed.put(lane, false);
        }
    }

    private static class OrbitHitMarker {
        private final Lane lane;
        private final double angle;
        private final double shrinkDuration;
        private double age;

        private OrbitHitMarker(Lane lane, double angle, double shrinkDuration) {
            this.lane = lane;
            this.angle = angle;
            this.shrinkDuration = shrinkDuration;
            this.age = 0.0;
        }
    }
}