package editor;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;

public class ChartEditor extends JFrame {

    private final LanePanel lanePanel;

    private Clip audioClip;
    private File audioFile;

    private final Timer playbackTimer;

    private JTextField startField;
    private JLabel musicLabel;

    private double playbackStartSeconds = 0.0;

    public ChartEditor() {
        setTitle("Rhythm Chart Editor");
        setSize(760, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        lanePanel = new LanePanel();

        add(createTopPanel(), BorderLayout.NORTH);
        add(lanePanel, BorderLayout.CENTER);

        playbackTimer = new Timer(16, e -> updatePlaybackLine());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopAudio();
                closeAudio();
            }
        });

        setLocationRelativeTo(null);
    }

    private JPanel createTopPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton loadChartButton = new JButton("Load Chart");
        JButton saveChartButton = new JButton("Save Chart");
        JButton clearButton = new JButton("Clear");

        JButton loadMusicButton = new JButton("Load Music");
        JButton playButton = new JButton("Play");
        JButton stopButton = new JButton("Stop");
        JButton exitButton = new JButton("Exit");

        JLabel startLabel = new JLabel("Start(s):");
        startField = new JTextField("0.0", 6);

        JButton useScrollButton = new JButton("Use Scroll");

        JLabel gridLabel = new JLabel("Grid:");
        String[] gridOptions = { "1.0", "0.5", "0.25", "0.125" };
        JComboBox<String> gridCombo = new JComboBox<>(gridOptions);
        gridCombo.setSelectedItem("0.25");

        JLabel speedLabel = new JLabel("Pixels/Sec:");
        JTextField speedField = new JTextField("120", 5);

        panel.add(loadChartButton);
        panel.add(saveChartButton);
        panel.add(clearButton);
        panel.add(Box.createHorizontalStrut(16));

        panel.add(loadMusicButton);
        panel.add(playButton);
        panel.add(stopButton);
        panel.add(exitButton);
        panel.add(Box.createHorizontalStrut(12));

        panel.add(startLabel);
        panel.add(startField);
        panel.add(useScrollButton);
        panel.add(Box.createHorizontalStrut(12));

        panel.add(gridLabel);
        panel.add(gridCombo);
        panel.add(Box.createHorizontalStrut(10));

        panel.add(speedLabel);
        panel.add(speedField);

        musicLabel = new JLabel("No music loaded");
        musicLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 6, 10));

        wrapper.add(panel, BorderLayout.NORTH);
        wrapper.add(musicLabel, BorderLayout.SOUTH);

        loadChartButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Load Chart");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                lanePanel.loadChart(chooser.getSelectedFile());
            }
        });

        saveChartButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Chart");
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                lanePanel.saveChart(chooser.getSelectedFile());
            }
        });

        clearButton.addActionListener(e -> lanePanel.clearNotes());

        loadMusicButton.addActionListener(e -> chooseAndLoadMusic());
        playButton.addActionListener(e -> playAudio());
        stopButton.addActionListener(e -> stopAudioAndResetLine());
        exitButton.addActionListener(e -> {
            stopAudio();
            closeAudio();
            dispose();
        });

        useScrollButton.addActionListener(e -> {
            startField.setText(String.format("%.3f", lanePanel.getScrollOffsetSeconds()));
        });

        gridCombo.addActionListener(e -> {
            try {
                double grid = Double.parseDouble((String) gridCombo.getSelectedItem());
                lanePanel.setGridInterval(grid);
            } catch (Exception ignored) {
            }
        });

        speedField.addActionListener(e -> {
            try {
                double speed = Double.parseDouble(speedField.getText().trim());
                if (speed > 0) {
                    lanePanel.setPixelsPerSecond(speed);
                }
            } catch (Exception ignored) {
            }
        });

        return wrapper;
    }

    private void chooseAndLoadMusic() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Music");
        chooser.setFileFilter(new FileNameExtensionFilter("WAV Audio (*.wav)", "wav"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadMusic(chooser.getSelectedFile());
        }
    }

    private void loadMusic(File file) {
        stopAudio();
        closeAudio();

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            audioClip = clip;
            audioFile = file;

            lanePanel.setPlaybackTimeSeconds(0.0);
            lanePanel.setPlaybackLineVisible(true);

            musicLabel.setText("Music: " + file.getName() + "  |  Length: " +
                    String.format("%.3f s", getClipLengthSeconds()));

        } catch (Exception e) {
            audioClip = null;
            audioFile = null;
            musicLabel.setText("No music loaded");
            JOptionPane.showMessageDialog(this,
                    "음악 파일 로드 실패: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void playAudio() {
        if (audioClip == null) {
            JOptionPane.showMessageDialog(this,
                    "먼저 음악 파일을 불러오세요.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double startSeconds = parseStartSeconds();
        double clipLength = getClipLengthSeconds();

        if (startSeconds < 0) {
            startSeconds = 0;
        }
        if (startSeconds > clipLength) {
            startSeconds = clipLength;
        }

        playbackStartSeconds = startSeconds;

        stopAudio();

        audioClip.setMicrosecondPosition((long) (startSeconds * 1_000_000L));

        lanePanel.setScrollOffsetSeconds(startSeconds);
        lanePanel.setPlaybackTimeSeconds(startSeconds);
        lanePanel.setPlaybackLineVisible(true);

        audioClip.start();
        playbackTimer.start();
        lanePanel.requestFocusInWindow();
    }

    private void stopAudio() {
        playbackTimer.stop();

        if (audioClip != null) {
            audioClip.stop();
        }
    }

    private void stopAudioAndResetLine() {
        stopAudio();

        if (audioClip != null) {
            audioClip.setMicrosecondPosition((long) (playbackStartSeconds * 1_000_000L));
        }

        lanePanel.setPlaybackTimeSeconds(playbackStartSeconds);
        lanePanel.setPlaybackLineVisible(audioClip != null);
        lanePanel.repaint();
    }

    private void closeAudio() {
        if (audioClip != null) {
            audioClip.stop();
            audioClip.close();
            audioClip = null;
        }
    }

    private void updatePlaybackLine() {
        if (audioClip == null) {
            playbackTimer.stop();
            return;
        }

        double currentSeconds = audioClip.getMicrosecondPosition() / 1_000_000.0;
        lanePanel.setPlaybackTimeSeconds(currentSeconds);

        if (!audioClip.isRunning()) {
            double clipLength = getClipLengthSeconds();
            if (currentSeconds >= clipLength - 0.01) {
                playbackTimer.stop();
            }
        }
    }

    private double getClipLengthSeconds() {
        if (audioClip == null) {
            return 0.0;
        }
        return audioClip.getMicrosecondLength() / 1_000_000.0;
    }

    private double parseStartSeconds() {
        try {
            return Double.parseDouble(startField.getText().trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChartEditor().setVisible(true));
    }
}

class LanePanel extends JPanel {

    private final ArrayList<Note> notes = new ArrayList<>();

    private final int lanes = 4;
    private final int laneWidth = 100;
    private final int noteWidth = 60;
    private final int noteHeight = 12;
    private final int baseX = 100;

    private double pixelsPerSecond = 120.0;
    private double gridInterval = 0.25;

    private double scrollOffsetSeconds = 0.0;
    private final double wheelScrollSeconds = 0.5;

    private double playbackTimeSeconds = 0.0;
    private boolean playbackLineVisible = false;

    public LanePanel() {
        setBackground(Color.BLACK);
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    toggleNoteByMouse(e);
                }
            }
        });

        addMouseWheelListener(e -> {
            scrollOffsetSeconds += e.getPreciseWheelRotation() * wheelScrollSeconds;

            if (scrollOffsetSeconds < 0) {
                scrollOffsetSeconds = 0;
            }

            repaint();
        });
    }

    private void toggleNoteByMouse(MouseEvent e) {
        int relativeX = e.getX() - baseX;

        if (relativeX < 0 || relativeX >= lanes * laneWidth) {
            return;
        }

        int lane = relativeX / laneWidth;

        double rawTime = screenYToTime(e.getY());
        double snappedTime = Math.round(rawTime / gridInterval) * gridInterval;

        if (snappedTime < 0) {
            snappedTime = 0;
        }

        Note existing = findNote(lane, snappedTime);

        if (existing != null) {
            notes.remove(existing);
        } else {
            notes.add(new Note(lane, snappedTime));
            sortNotes();
        }

        repaint();
    }

    private Note findNote(int lane, double time) {
        final double epsilon = 0.0001;

        for (Note note : notes) {
            if (note.lane == lane && Math.abs(note.time - time) < epsilon) {
                return note;
            }
        }

        return null;
    }

    private void sortNotes() {
        notes.sort(Comparator.comparingDouble((Note n) -> n.time).thenComparingInt(n -> n.lane));
    }

    private double screenYToTime(int y) {
        return scrollOffsetSeconds + (y / pixelsPerSecond);
    }

    private int timeToScreenY(double time) {
        return (int) Math.round((time - scrollOffsetSeconds) * pixelsPerSecond);
    }

    public void setGridInterval(double gridInterval) {
        this.gridInterval = gridInterval;
        repaint();
    }

    public void setPixelsPerSecond(double pixelsPerSecond) {
        this.pixelsPerSecond = pixelsPerSecond;
        repaint();
    }

    public void setPlaybackTimeSeconds(double playbackTimeSeconds) {
        this.playbackTimeSeconds = playbackTimeSeconds;
        repaint();
    }

    public void setPlaybackLineVisible(boolean playbackLineVisible) {
        this.playbackLineVisible = playbackLineVisible;
        repaint();
    }

    public void setScrollOffsetSeconds(double scrollOffsetSeconds) {
        this.scrollOffsetSeconds = Math.max(0.0, scrollOffsetSeconds);
        repaint();
    }

    public double getScrollOffsetSeconds() {
        return scrollOffsetSeconds;
    }

    public void clearNotes() {
        notes.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            drawLanes(g2);
            drawGrid(g2);
            drawLaneBorders(g2);
            drawPlaybackLine(g2);
            drawNotes(g2);
            drawInfo(g2);
        } finally {
            g2.dispose();
        }
    }

    private void drawLanes(Graphics2D g) {
        for (int i = 0; i < lanes; i++) {
            int x = baseX + i * laneWidth;

            g.setColor(new Color(30, 30, 30));
            g.fillRect(x, 0, laneWidth, getHeight());

            g.setColor(new Color(40, 40, 40));
            g.fillRect(x + 4, 0, laneWidth - 8, getHeight());
        }
    }

    private void drawGrid(Graphics2D g) {
        int panelHeight = getHeight();
        int x1 = baseX;
        int x2 = baseX + lanes * laneWidth;

        double startTime = scrollOffsetSeconds;
        double endTime = scrollOffsetSeconds + (panelHeight / pixelsPerSecond);

        double firstGrid = Math.floor(startTime / gridInterval) * gridInterval;

        g.setFont(new Font("Arial", Font.PLAIN, 12));

        for (double t = firstGrid; t <= endTime + gridInterval; t += gridInterval) {
            int y = timeToScreenY(t);

            boolean major = Math.abs(t - Math.round(t)) < 0.0001;

            if (major) {
                g.setColor(new Color(120, 120, 120));
            } else {
                g.setColor(new Color(75, 75, 75));
            }

            g.drawLine(x1, y, x2, y);

            if (major) {
                g.setColor(Color.WHITE);
                String label = String.format("%.0f", t);
                FontMetrics fm = g.getFontMetrics();
                int textX = baseX - 10 - fm.stringWidth(label);
                int textY = y + (fm.getAscent() / 2) - 2;
                g.drawString(label, textX, textY);
            }
        }
    }

    private void drawLaneBorders(Graphics2D g) {
        g.setColor(Color.GRAY);

        for (int i = 0; i <= lanes; i++) {
            int x = baseX + i * laneWidth;
            g.drawLine(x, 0, x, getHeight());
        }
    }

    private void drawPlaybackLine(Graphics2D g) {
        if (!playbackLineVisible) {
            return;
        }

        int y = timeToScreenY(playbackTimeSeconds);

        if (y < -4 || y > getHeight() + 4) {
            return;
        }

        int x1 = baseX;
        int x2 = baseX + lanes * laneWidth;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(Color.WHITE);
            g2.drawLine(x1, y, x2, y);

            g2.setFont(new Font("Arial", Font.BOLD, 12));
            String label = String.format("PLAY %.3f s", playbackTimeSeconds);
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
        g.setColor(Color.CYAN);

        for (Note note : notes) {
            int y = timeToScreenY(note.time) - noteHeight / 2;

            if (y + noteHeight < 0 || y > getHeight()) {
                continue;
            }

            int x = baseX + note.lane * laneWidth + (laneWidth - noteWidth) / 2;
            g.fillRect(x, y, noteWidth, noteHeight);
        }
    }

    private void drawInfo(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));

        g.drawString(String.format("Scroll: %.3f s", scrollOffsetSeconds), 10, 20);
        g.drawString(String.format("Grid: %.3f", gridInterval), 10, 40);
        g.drawString(String.format("Cursor: %.3f s", playbackTimeSeconds), 10, 60);
    }

    public void saveChart(File file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            sortNotes();

            for (Note note : notes) {
                String laneText = switch (note.lane) {
                    case 0 -> "D";
                    case 1 -> "F";
                    case 2 -> "J";
                    case 3 -> "K";
                    default -> throw new IllegalStateException("Unexpected lane: " + note.lane);
                };

                bw.write(String.format("%.3f %s%n", note.time, laneText));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "저장 실패: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadChart(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            notes.clear();

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    continue;
                }

                double time;
                try {
                    time = Double.parseDouble(parts[0]);
                } catch (NumberFormatException e) {
                    continue;
                }

                int lane = switch (parts[1]) {
                    case "D" -> 0;
                    case "F" -> 1;
                    case "J" -> 2;
                    case "K" -> 3;
                    default -> -1;
                };

                if (lane == -1) {
                    continue;
                }

                notes.add(new Note(lane, time));
            }

            sortNotes();
            repaint();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "불러오기 실패: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

class Note {
    int lane;
    double time;

    public Note(int lane, double time) {
        this.lane = lane;
        this.time = time;
    }
}