package editor;

import editor.NoteData;
import state.gameplay.KeyMode;
import state.gameplay.Lane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class LanePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ArrayList<NoteData> notes = new ArrayList<>();

    private KeyMode keyMode = KeyMode.KEY_4;
    private List<Lane> lanes = new ArrayList<>(Arrays.asList(KeyMode.KEY_4.getLanes()));

    private final int sidebarWidth = 180;
    private final int minLaneWidth = 64;
    private final int preferredLaneWidth = 100;
    private final int noteHeight = 12;
    private final int timelineTopPadding = 18;

    private double pixelsPerBeat = 64.0;
    private double gridInterval = 0.25;

    private double scrollOffsetBeats = 0.0;
    private final double wheelScrollBeats = 1.0;

    private double playbackBeat = 0.0;
    private boolean playbackLineVisible = false;

    private Integer pendingLongLaneIndex = null;
    private Double pendingLongStartBeat = null;

    public LanePanel() {
        setBackground(Color.BLACK);
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.isShiftDown()) {
                        placeLongNoteByMouse(e);
                    } else {
                        toggleTapNoteByMouse(e);
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    removeNoteByMouse(e);
                }
            }
        });

        addMouseWheelListener(e -> {
            scrollOffsetBeats += e.getPreciseWheelRotation() * wheelScrollBeats;

            if (scrollOffsetBeats < 0) {
                scrollOffsetBeats = 0;
            }

            repaint();
        });
    }

    public void setKeyMode(KeyMode keyMode) {
        if (keyMode == null) {
            return;
        }

        if (this.keyMode == keyMode) {
            return;
        }

        this.keyMode = keyMode;
        this.lanes = new ArrayList<>(Arrays.asList(keyMode.getLanes()));

        notes.removeIf(note -> note.laneIndex < 0 || note.laneIndex >= lanes.size());
        clearPendingLongAnchor();
        repaint();
    }

    private void toggleTapNoteByMouse(MouseEvent e) {
        NotePlacement placement = resolvePlacement(e);
        if (placement == null) {
            return;
        }

        clearPendingLongAnchor();

        NoteData existing = findNote(placement.laneIndex, placement.snappedBeat);
        if (existing != null) {
            notes.remove(existing);
        } else {
            notes.add(new NoteData(placement.laneIndex, placement.snappedBeat));
            sortNotes();
        }

        repaint();
    }

    private void placeLongNoteByMouse(MouseEvent e) {
        NotePlacement placement = resolvePlacement(e);
        if (placement == null) {
            return;
        }

        if (pendingLongLaneIndex == null || pendingLongStartBeat == null
                || pendingLongLaneIndex != placement.laneIndex) {
            pendingLongLaneIndex = placement.laneIndex;
            pendingLongStartBeat = placement.snappedBeat;
            repaint();
            return;
        }

        if (Math.abs(pendingLongStartBeat - placement.snappedBeat) < 0.0001) {
            pendingLongLaneIndex = placement.laneIndex;
            pendingLongStartBeat = placement.snappedBeat;
            repaint();
            return;
        }

        double startBeat = Math.min(pendingLongStartBeat, placement.snappedBeat);
        double endBeat = Math.max(pendingLongStartBeat, placement.snappedBeat);

        removeOverlappingNotes(placement.laneIndex, startBeat, endBeat);
        notes.add(new NoteData(placement.laneIndex, startBeat, endBeat));
        sortNotes();
        clearPendingLongAnchor();
        repaint();
    }

    private void removeNoteByMouse(MouseEvent e) {
        NotePlacement placement = resolvePlacement(e);
        if (placement == null) {
            clearPendingLongAnchor();
            repaint();
            return;
        }

        NoteData existing = findNote(placement.laneIndex, placement.snappedBeat);
        if (existing != null) {
            notes.remove(existing);
        }

        clearPendingLongAnchor();
        repaint();
    }

    private NotePlacement resolvePlacement(MouseEvent e) {
        int laneWidth = getLaneWidth();
        int baseX = getBaseX();
        int relativeX = e.getX() - baseX;

        if (relativeX < 0 || relativeX >= getTrackWidth()) {
            return null;
        }

        int laneIndex = relativeX / laneWidth;
        if (laneIndex < 0 || laneIndex >= lanes.size()) {
            return null;
        }

        double rawBeat = screenYToBeat(e.getY());
        double snappedBeat = Math.round(rawBeat / gridInterval) * gridInterval;

        if (snappedBeat < 0) {
            snappedBeat = 0;
        }

        return new NotePlacement(laneIndex, snappedBeat);
    }

    private void clearPendingLongAnchor() {
        pendingLongLaneIndex = null;
        pendingLongStartBeat = null;
    }

    private void removeOverlappingNotes(int laneIndex, double startBeat, double endBeat) {
        notes.removeIf(note -> note.laneIndex == laneIndex && note.overlaps(startBeat, endBeat));
    }

    private NoteData findNote(int laneIndex, double beat) {
        final double epsilon = 0.0001;

        for (NoteData note : notes) {
            if (note.laneIndex != laneIndex) {
                continue;
            }

            if (note.containsBeat(beat, epsilon)) {
                return note;
            }
        }

        return null;
    }

    private void sortNotes() {
        notes.sort(Comparator.comparingDouble((NoteData n) -> n.beat).thenComparingDouble(n -> n.endBeat)
                .thenComparingInt(n -> n.laneIndex));
    }

    private double screenYToBeat(int y) {
        return scrollOffsetBeats + ((y - timelineTopPadding) / pixelsPerBeat);
    }

    private int beatToScreenY(double beat) {
        return timelineTopPadding + (int) Math.round((beat - scrollOffsetBeats) * pixelsPerBeat);
    }

    private int getLaneWidth() {
        int availableWidth = Math.max(getWidth() - sidebarWidth - 40, minLaneWidth * lanes.size());
        int dynamicLaneWidth = availableWidth / Math.max(1, lanes.size());
        return Math.max(minLaneWidth, Math.min(preferredLaneWidth, dynamicLaneWidth));
    }

    private int getTrackWidth() {
        return getLaneWidth() * lanes.size();
    }

    private int getBaseX() {
        int centeredX = (getWidth() - getTrackWidth()) / 2;
        return Math.max(sidebarWidth, centeredX);
    }

    private int getNoteWidth() {
        return Math.max(32, getLaneWidth() - 40);
    }

    public void setGridInterval(double gridInterval) {
        this.gridInterval = gridInterval;
        repaint();
    }

    public void setPixelsPerBeat(double pixelsPerBeat) {
        this.pixelsPerBeat = pixelsPerBeat;
        repaint();
    }

    public void setPlaybackBeat(double playbackBeat) {
        this.playbackBeat = playbackBeat;
        repaint();
    }

    public void setPlaybackLineVisible(boolean playbackLineVisible) {
        this.playbackLineVisible = playbackLineVisible;
        repaint();
    }

    public void setScrollOffsetBeats(double scrollOffsetBeats) {
        this.scrollOffsetBeats = Math.max(0.0, scrollOffsetBeats);
        repaint();
    }

    public double getScrollOffsetBeats() {
        return scrollOffsetBeats;
    }

    public int getNoteCount() {
        return notes.size();
    }

    public int getLaneCount() {
        return lanes.size();
    }

    public void clearNotes() {
        notes.clear();
        clearPendingLongAnchor();
        repaint();
    }

    public ArrayList<String> exportChartLines() {
        ArrayList<String> lines = new ArrayList<>();
        sortNotes();

        for (NoteData note : notes) {
            Lane lane = getLaneByIndex(note.laneIndex);
            if (lane == null) {
                continue;
            }

            if (note.isLongNote()) {
                lines.add(String.format("%.3f %.3f %s", note.beat, note.endBeat, lane.getChartToken()));
            } else {
                lines.add(String.format("%.3f %s", note.beat, lane.getChartToken()));
            }
        }

        return lines;
    }

    public void loadChartLines(List<String> lines) {
        notes.clear();
        clearPendingLongAnchor();

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

            double beat;
            try {
                beat = Double.parseDouble(parts[0]);
            } catch (NumberFormatException e) {
                continue;
            }

            double endBeat = beat;
            int laneTokenStartIndex = 1;

            if (parts.length >= 3) {
                try {
                    endBeat = Double.parseDouble(parts[1]);
                    laneTokenStartIndex = 2;
                } catch (NumberFormatException ignored) {
                    endBeat = beat;
                    laneTokenStartIndex = 1;
                }
            }

            if (laneTokenStartIndex >= parts.length) {
                continue;
            }

            String laneToken = String.join(" ", Arrays.copyOfRange(parts, laneTokenStartIndex, parts.length));
            Lane lane = keyMode.fromChartToken(laneToken);
            int laneIndex = getLaneIndex(lane);

            if (laneIndex < 0) {
                continue;
            }

            notes.add(new NoteData(laneIndex, beat, Math.max(beat, endBeat)));
        }

        sortNotes();
        repaint();
    }

    private Lane getLaneByIndex(int laneIndex) {
        if (laneIndex < 0 || laneIndex >= lanes.size()) {
            return null;
        }
        return lanes.get(laneIndex);
    }

    private int getLaneIndex(Lane lane) {
        if (lane == null) {
            return -1;
        }
        return lanes.indexOf(lane);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            drawLanes(g2);
            drawGrid(g2);
            drawLaneBorders(g2);
            drawLaneLabels(g2);
            drawPlaybackLine(g2);
            drawNotes(g2);
            drawPendingLongAnchor(g2);
            drawInfo(g2);
        } finally {
            g2.dispose();
        }
    }

    private void drawLanes(Graphics2D g) {
        int laneWidth = getLaneWidth();
        int baseX = getBaseX();

        for (int i = 0; i < lanes.size(); i++) {
            int x = baseX + i * laneWidth;

            g.setColor(new Color(30, 30, 30));
            g.fillRect(x, 0, laneWidth, getHeight());

            g.setColor(new Color(40, 40, 40));
            g.fillRect(x + 4, 0, laneWidth - 8, getHeight());
        }
    }

    private void drawGrid(Graphics2D g) {
        int panelHeight = getHeight();
        int baseX = getBaseX();
        int x1 = baseX;
        int x2 = baseX + getTrackWidth();

        double startBeat = scrollOffsetBeats;
        double endBeat = scrollOffsetBeats + ((panelHeight - timelineTopPadding) / pixelsPerBeat);
        double firstGrid = Math.floor(startBeat / gridInterval) * gridInterval;

        g.setFont(new Font("Arial", Font.PLAIN, 12));

        for (double beat = firstGrid; beat <= endBeat + gridInterval; beat += gridInterval) {
            int y = beatToScreenY(beat);

            if (y < timelineTopPadding - 2 || y > panelHeight + 2) {
                continue;
            }

            boolean major = Math.abs(beat - Math.round(beat)) < 0.0001;

            g.setColor(major ? new Color(120, 120, 120) : new Color(75, 75, 75));
            g.drawLine(x1, y, x2, y);

            if (major) {
                g.setColor(Color.WHITE);
                String label = String.format("%.0f", beat);
                FontMetrics fm = g.getFontMetrics();
                int textX = baseX - 10 - fm.stringWidth(label);
                int textY = y + (fm.getAscent() / 2) - 2;
                g.drawString(label, textX, textY);
            }
        }
    }

    private void drawLaneBorders(Graphics2D g) {
        int laneWidth = getLaneWidth();
        int baseX = getBaseX();
        g.setColor(Color.GRAY);

        for (int i = 0; i <= lanes.size(); i++) {
            int x = baseX + i * laneWidth;
            g.drawLine(x, 0, x, getHeight());
        }
    }

    private void drawLaneLabels(Graphics2D g) {
        int laneWidth = getLaneWidth();
        int baseX = getBaseX();

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, Math.max(12, Math.min(18, laneWidth / 5))));

        for (int i = 0; i < lanes.size(); i++) {
            int x = baseX + i * laneWidth;
            String label = lanes.get(i).getDisplayName();
            FontMetrics fm = g.getFontMetrics();
            int textX = x + (laneWidth - fm.stringWidth(label)) / 2;
            g.drawString(label, textX, 24);
        }
    }

    private void drawPlaybackLine(Graphics2D g) {
        if (!playbackLineVisible) {
            return;
        }

        int y = beatToScreenY(playbackBeat);
        if (y < -4 || y > getHeight() + 4) {
            return;
        }

        int x1 = getBaseX();
        int x2 = x1 + getTrackWidth();

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(Color.WHITE);
            g2.drawLine(x1, y, x2, y);

            g2.setFont(new Font("Arial", Font.BOLD, 12));
            String label = String.format("PLAY %.3f beat", playbackBeat);
            FontMetrics fm = g2.getFontMetrics();
            int boxW = fm.stringWidth(label) + 10;
            int boxH = 18;

            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(x2 + 8, y - boxH + 6, boxW, boxH);

            g2.setColor(Color.WHITE);
            g2.drawString(label, x2 + 13, y);
        } finally {
            g2.dispose();
        }
    }

    private void drawNotes(Graphics2D g) {
        int laneWidth = getLaneWidth();
        int noteWidth = getNoteWidth();
        int baseX = getBaseX();

        for (NoteData note : notes) {
            int laneX = baseX + note.laneIndex * laneWidth;
            int x = laneX + (laneWidth - noteWidth) / 2;
            int startY = beatToScreenY(note.beat) - noteHeight / 2;

            if (note.isLongNote()) {
                int endY = beatToScreenY(note.endBeat) - noteHeight / 2;
                int topY = Math.min(startY, endY);
                int bottomY = Math.max(startY, endY);
                int bodyX = x + Math.max(2, noteWidth / 6);
                int bodyWidth = Math.max(10, noteWidth - Math.max(4, noteWidth / 3));
                int bodyTop = topY + noteHeight / 2;
                int bodyHeight = Math.max(noteHeight, bottomY - topY);

                g.setColor(new Color(120, 220, 255));
                g.fillRoundRect(bodyX, bodyTop, bodyWidth, bodyHeight, 10, 10);

                g.setColor(new Color(210, 245, 255));
                g.fillRect(bodyX + 3, bodyTop, Math.max(3, bodyWidth / 5), bodyHeight);

                g.setColor(Color.CYAN);
                g.fillRoundRect(x, endY, noteWidth, noteHeight, 10, 10);
                g.setColor(Color.WHITE);
                g.drawRoundRect(x, endY, noteWidth, noteHeight, 10, 10);
            }

            if (startY + noteHeight < 0 || startY > getHeight()) {
                continue;
            }

            g.setColor(Color.CYAN);
            g.fillRoundRect(x, startY, noteWidth, noteHeight, 10, 10);
            g.setColor(Color.WHITE);
            g.drawRoundRect(x, startY, noteWidth, noteHeight, 10, 10);
        }
    }

    private void drawPendingLongAnchor(Graphics2D g) {
        if (pendingLongLaneIndex == null || pendingLongStartBeat == null) {
            return;
        }

        int laneWidth = getLaneWidth();
        int noteWidth = getNoteWidth();
        int baseX = getBaseX();
        int laneX = baseX + pendingLongLaneIndex * laneWidth;
        int x = laneX + (laneWidth - noteWidth) / 2;
        int y = beatToScreenY(pendingLongStartBeat) - noteHeight / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Color.ORANGE);
            g2.drawRoundRect(x - 2, y - 2, noteWidth + 4, noteHeight + 4, 10, 10);
            g2.drawString("LN START", x + noteWidth + 8, y + 10);
        } finally {
            g2.dispose();
        }
    }

    private void drawInfo(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));

        g.drawString(String.format("Scroll: %.3f beat", scrollOffsetBeats), 10, 20);
        g.drawString(String.format("Grid: %.3f beat", gridInterval), 10, 40);
        g.drawString(String.format("Cursor: %.3f beat", playbackBeat), 10, 60);
        g.drawString(String.format("Key Mode: %dK", keyMode.getKeyCount()), 10, 80);
        g.drawString(String.format("Lanes: %d", lanes.size()), 10, 100);
        g.drawString("Left Click: Add/Remove Tap", 10, 130);
        g.drawString("Shift + Left Click x2: Create Long Note", 10, 150);
        g.drawString("Right Click: Remove Note", 10, 170);
        g.drawString("Mouse Wheel: Scroll", 10, 190);
    }

    public void saveChart(File file) {
        try {
            ArrayList<String> lines = exportChartLines();
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "저장 실패: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadChart(File file) {
        try {
            loadChartLines(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "불러오기 실패: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class NotePlacement {
        final int laneIndex;
        final double snappedBeat;

        private NotePlacement(int laneIndex, double snappedBeat) {
            this.laneIndex = laneIndex;
            this.snappedBeat = snappedBeat;
        }
    }
}