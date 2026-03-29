package editor;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import stage.Difficulty;
import stage.Stage;
import stage.StageManager;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChartEditor extends JFrame {

    private final LanePanel lanePanel;
    private final Timer playbackTimer;

    private final Map<String, StagePreset> presetMap = new LinkedHashMap<>();
    private final Map<String, byte[]> preloadedMusicBytes = new LinkedHashMap<>();
    private final Map<String, ArrayList<String>> preloadedCharts = new LinkedHashMap<>();

    private JComboBox<StagePreset> stageCombo;
    private JComboBox<Difficulty> difficultyCombo;

    private JTextField bpmField;
    private JTextField offsetField;
    private JTextField startBeatField;
    private JLabel musicLabel;

    private Clip audioClip;
    private double playbackStartBeat = 0.0;
    private boolean updatingPresetSelectors = false;

    public ChartEditor() {
        setTitle("Rhythm Chart Editor");
        setSize(1040, 820);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        lanePanel = new LanePanel();

        preloadStageAssets();

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

        initializePresetSelection();
        setLocationRelativeTo(null);
    }

    private JPanel createTopPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton loadChartButton = new JButton("Load Chart File");
        JButton saveChartButton = new JButton("Save Chart As");
        JButton overwritePresetButton = new JButton("Overwrite Preset");
        JButton clearButton = new JButton("Clear");

        stageCombo = new JComboBox<>();
        difficultyCombo = new JComboBox<>();
        JButton reloadPresetButton = new JButton("Reload Preset");

        JButton playButton = new JButton("Play");
        JButton stopButton = new JButton("Stop");
        JButton exitButton = new JButton("Exit");

        JLabel bpmLabel = new JLabel("BPM:");
        bpmField = new JTextField("160", 5);

        JLabel offsetLabel = new JLabel("Offset(s):");
        offsetField = new JTextField("0.0", 6);

        JLabel startBeatLabel = new JLabel("Start Beat:");
        startBeatField = new JTextField("0.0", 6);

        JButton useScrollButton = new JButton("Use Scroll");

        JLabel gridLabel = new JLabel("Grid:");
        String[] gridOptions = { "1.0", "0.5", "0.25", "0.125" };
        JComboBox<String> gridCombo = new JComboBox<>(gridOptions);
        gridCombo.setSelectedItem("0.25");

        JLabel speedLabel = new JLabel("Pixels/Beat:");
        JTextField speedField = new JTextField("64", 5);

        panel.add(loadChartButton);
        panel.add(saveChartButton);
        panel.add(overwritePresetButton);
        panel.add(clearButton);
        panel.add(Box.createHorizontalStrut(12));

        panel.add(new JLabel("Stage:"));
        panel.add(stageCombo);
        panel.add(new JLabel("Difficulty:"));
        panel.add(difficultyCombo);
        panel.add(reloadPresetButton);
        panel.add(Box.createHorizontalStrut(12));

        panel.add(playButton);
        panel.add(stopButton);
        panel.add(exitButton);
        panel.add(Box.createHorizontalStrut(12));

        panel.add(bpmLabel);
        panel.add(bpmField);
        panel.add(offsetLabel);
        panel.add(offsetField);
        panel.add(startBeatLabel);
        panel.add(startBeatField);
        panel.add(useScrollButton);
        panel.add(Box.createHorizontalStrut(12));

        panel.add(gridLabel);
        panel.add(gridCombo);
        panel.add(Box.createHorizontalStrut(10));

        panel.add(speedLabel);
        panel.add(speedField);

        musicLabel = new JLabel("No preset loaded");
        musicLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 6, 10));

        wrapper.add(panel, BorderLayout.NORTH);
        wrapper.add(musicLabel, BorderLayout.SOUTH);

        loadChartButton.addActionListener(e -> importChartFromFile());

        saveChartButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Chart As");
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                lanePanel.saveChart(chooser.getSelectedFile());
                updatePresetStatusLabel();
            }
        });

        overwritePresetButton.addActionListener(e -> overwriteSelectedPresetChart());

        clearButton.addActionListener(e -> {
            lanePanel.clearNotes();
            updatePresetStatusLabel();
        });

        stageCombo.addActionListener(e -> {
            if (updatingPresetSelectors) {
                return;
            }
            refreshDifficultyCombo(getSelectedPreset(), null);
            loadSelectedPreset();
        });

        difficultyCombo.addActionListener(e -> {
            if (updatingPresetSelectors) {
                return;
            }
            loadSelectedPreset();
        });

        reloadPresetButton.addActionListener(e -> loadSelectedPreset());
        playButton.addActionListener(e -> playAudio());
        stopButton.addActionListener(e -> stopAudioAndResetLine());
        exitButton.addActionListener(e -> {
            stopAudio();
            closeAudio();
            dispose();
        });

        useScrollButton.addActionListener(e ->
                startBeatField.setText(String.format("%.3f", lanePanel.getScrollOffsetBeats()))
        );

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
                    lanePanel.setPixelsPerBeat(speed);
                }
            } catch (Exception ignored) {
            }
        });

        registerKeyboardShortcuts(overwritePresetButton);
        return wrapper;
    }

    private void registerKeyboardShortcuts(JButton overwritePresetButton) {
        JRootPane rootPane = getRootPane();
        if (rootPane == null) {
            return;
        }

        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlS, "overwritePreset");
        rootPane.getActionMap().put("overwritePreset", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                overwritePresetButton.doClick();
            }
        });
    }

    private void preloadStageAssets() {
        presetMap.clear();
        preloadedMusicBytes.clear();
        preloadedCharts.clear();

        for (Stage stage : loadStagesFromManager()) {
            StagePreset preset = new StagePreset(stage);

            byte[] musicBytes = readBinaryResource(stage.getMusicPath());
            if (musicBytes != null) {
                preloadedMusicBytes.put(stage.getLevelName(), musicBytes);
            }

            for (Difficulty difficulty : Difficulty.values()) {
                String chartResourcePath = buildChartResourcePath(stage, difficulty);
                ArrayList<String> chartLines = readTextResource(chartResourcePath);

                if (chartLines != null) {
                    String chartCacheKey = buildChartCacheKey(stage, difficulty);
                    preloadedCharts.put(chartCacheKey, chartLines);
                    preset.addAvailableDifficulty(difficulty);
                }
            }

            if (preset.hasAnyDifficulty()) {
                presetMap.put(stage.getLevelName(), preset);
            }
        }
    }

    private List<Stage> loadStagesFromManager() {
        ArrayList<Stage> stages = new ArrayList<>();
        StageManager manager = new StageManager();

        int count = manager.getStageSize();
        for (int i = 0; i < count; i++) {
            stages.add(manager.getCurrentStage());
            if (manager.hasNext()) {
                manager.next();
            }
        }

        return stages;
    }

    private void initializePresetSelection() {
        updatingPresetSelectors = true;
        stageCombo.removeAllItems();

        for (StagePreset preset : presetMap.values()) {
            stageCombo.addItem(preset);
        }

        updatingPresetSelectors = false;

        if (stageCombo.getItemCount() == 0) {
            musicLabel.setText("프리로드 가능한 레벨 채보가 없습니다.");
            difficultyCombo.setEnabled(false);
            return;
        }

        stageCombo.setSelectedIndex(0);
        refreshDifficultyCombo(getSelectedPreset(), Difficulty.Easy);
        loadSelectedPreset();
    }

    private void refreshDifficultyCombo(StagePreset preset, Difficulty preferredDifficulty) {
        updatingPresetSelectors = true;
        difficultyCombo.removeAllItems();

        if (preset != null) {
            for (Difficulty difficulty : Difficulty.values()) {
                if (preset.hasDifficulty(difficulty)) {
                    difficultyCombo.addItem(difficulty);
                }
            }
        }

        if (difficultyCombo.getItemCount() > 0) {
            Difficulty toSelect = preferredDifficulty;
            if (toSelect == null || !preset.hasDifficulty(toSelect)) {
                toSelect = (Difficulty) difficultyCombo.getItemAt(0);
            }
            difficultyCombo.setSelectedItem(toSelect);
            difficultyCombo.setEnabled(true);
        } else {
            difficultyCombo.setEnabled(false);
        }

        updatingPresetSelectors = false;
    }

    private void loadSelectedPreset() {
        StagePreset preset = getSelectedPreset();
        Difficulty difficulty = getSelectedDifficulty();

        if (preset == null || difficulty == null) {
            return;
        }

        stopAudio();
        closeAudio();

        bpmField.setText(formatDouble(preset.stage.getMusicBPM()));
        offsetField.setText(formatDouble(preset.stage.getMusicOffsetSeconds()));

        ArrayList<String> chartLines = preloadedCharts.get(buildChartCacheKey(preset.stage, difficulty));
        if (chartLines != null) {
            lanePanel.loadChartLines(chartLines);
        } else {
            lanePanel.clearNotes();
        }

        byte[] musicBytes = preloadedMusicBytes.get(preset.stage.getLevelName());
        if (musicBytes != null) {
            try {
                audioClip = createClipFromBytes(musicBytes);
                lanePanel.setPlaybackBeat(0.0);
                lanePanel.setPlaybackLineVisible(true);
            } catch (Exception e) {
                audioClip = null;
                lanePanel.setPlaybackLineVisible(false);
                JOptionPane.showMessageDialog(this,
                        "프리로드된 음악 열기 실패: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            audioClip = null;
            lanePanel.setPlaybackLineVisible(false);
        }

        playbackStartBeat = 0.0;
        startBeatField.setText("0.0");
        updatePresetStatusLabel();
        lanePanel.requestFocusInWindow();
    }

    private void importChartFromFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Chart");
        chooser.setFileFilter(new FileNameExtensionFilter("Text Chart (*.txt)", "txt"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            lanePanel.loadChart(chooser.getSelectedFile());
            updatePresetStatusLabel();
        }
    }

    private void overwriteSelectedPresetChart() {
        StagePreset preset = getSelectedPreset();
        Difficulty difficulty = getSelectedDifficulty();

        if (preset == null || difficulty == null) {
            JOptionPane.showMessageDialog(this,
                    "선택된 프리셋이 없습니다.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String fileName = buildChartFileName(preset.stage, difficulty);
        Path targetPath = resolveWritableChartPath(preset.stage, difficulty);

        if (targetPath == null) {
            JOptionPane.showMessageDialog(this,
                    "원본 채보 파일 경로를 찾지 못했습니다.\n"
                            + "아래 이름으로 asset 폴더에 파일을 두고 다시 시도하세요:\n"
                            + fileName,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        ArrayList<String> lines = lanePanel.exportChartLines();

        try {
            Path parent = targetPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(targetPath, lines, StandardCharsets.UTF_8);
            preloadedCharts.put(buildChartCacheKey(preset.stage, difficulty), new ArrayList<>(lines));
            updatePresetStatusLabel();

            JOptionPane.showMessageDialog(this,
                    "프리셋 원본 채보에 바로 저장했습니다.\n" + targetPath.toAbsolutePath(),
                    "Saved",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "프리셋 덮어쓰기 실패: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private Path resolveWritableChartPath(Stage stage, Difficulty difficulty) {
        String resourcePath = buildChartResourcePath(stage, difficulty);
        String fileName = buildChartFileName(stage, difficulty);

        ArrayList<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get("asset", fileName));
        candidates.add(Paths.get("Asset", fileName));
        candidates.add(Paths.get("src", "asset", fileName));
        candidates.add(Paths.get("src", "main", "resources", fileName));
        candidates.add(Paths.get("resources", fileName));

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            Path parent = normalized.getParent();

            if (Files.exists(normalized)) {
                return normalized;
            }
            if (parent != null && Files.exists(parent) && Files.isDirectory(parent)) {
                return normalized;
            }
        }

        try {
            URL resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl != null && "file".equalsIgnoreCase(resourceUrl.getProtocol())) {
                return Paths.get(resourceUrl.toURI()).toAbsolutePath().normalize();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private Clip createClipFromBytes(byte[] audioBytes) throws Exception {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                new BufferedInputStream(new ByteArrayInputStream(audioBytes)))) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            return clip;
        }
    }

    private void playAudio() {
        if (audioClip == null) {
            JOptionPane.showMessageDialog(this,
                    "선택한 레벨의 음악이 프리로드되지 않았습니다.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double bpm = parseBpm();
        double offsetSeconds = parseOffsetSeconds();
        double startBeat = parseStartBeat();

        playbackStartBeat = startBeat;

        double startSeconds = beatToSeconds(startBeat, bpm, offsetSeconds);
        double clipLength = getClipLengthSeconds();

        if (startSeconds < 0.0) {
            startSeconds = 0.0;
        }
        if (startSeconds > clipLength) {
            startSeconds = clipLength;
        }

        stopAudio();

        audioClip.setMicrosecondPosition((long) (startSeconds * 1_000_000L));

        lanePanel.setScrollOffsetBeats(startBeat);
        lanePanel.setPlaybackBeat(startBeat);
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
            double bpm = parseBpm();
            double offsetSeconds = parseOffsetSeconds();
            double startSeconds = beatToSeconds(playbackStartBeat, bpm, offsetSeconds);

            if (startSeconds < 0.0) {
                startSeconds = 0.0;
            }
            if (startSeconds > getClipLengthSeconds()) {
                startSeconds = getClipLengthSeconds();
            }

            audioClip.setMicrosecondPosition((long) (startSeconds * 1_000_000L));
        }

        lanePanel.setPlaybackBeat(playbackStartBeat);
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

        double bpm = parseBpm();
        double offsetSeconds = parseOffsetSeconds();

        double currentSeconds = audioClip.getMicrosecondPosition() / 1_000_000.0;
        double currentBeat = secondsToBeat(currentSeconds, bpm, offsetSeconds);

        lanePanel.setPlaybackBeat(currentBeat);

        if (!audioClip.isRunning()) {
            double clipLength = getClipLengthSeconds();
            if (currentSeconds >= clipLength - 0.01) {
                playbackTimer.stop();
            }
        }
    }

    private void updatePresetStatusLabel() {
        StagePreset preset = getSelectedPreset();
        Difficulty difficulty = getSelectedDifficulty();

        if (preset == null || difficulty == null) {
            musicLabel.setText("No preset loaded");
            return;
        }

        String musicStatus = audioClip != null
                ? String.format("%.3f s", getClipLengthSeconds())
                : "missing";

        musicLabel.setText(String.format(
                "Preset: %s / %s | BPM: %s | Offset: %s s | Notes: %d | Music: %s | Ctrl+S: overwrite",
                preset.stage.getLevelName(),
                difficulty.name(),
                formatDouble(parseBpm()),
                formatDouble(parseOffsetSeconds()),
                lanePanel.getNoteCount(),
                musicStatus
        ));
    }

    private String buildChartResourcePath(Stage stage, Difficulty difficulty) {
        return "/" + buildChartFileName(stage, difficulty);
    }

    private String buildChartFileName(Stage stage, Difficulty difficulty) {
        return stage.getLevelName() + "_" + difficulty.name() + ".txt";
    }

    private String buildChartCacheKey(Stage stage, Difficulty difficulty) {
        return stage.getLevelName() + "::" + difficulty.name();
    }

    private byte[] readBinaryResource(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            return out.toByteArray();
        } catch (Exception e) {
            System.err.println("[ChartEditor] Failed to preload music: " + path);
            e.printStackTrace();
            return null;
        }
    }

    private ArrayList<String> readTextResource(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }

            ArrayList<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            }

            return lines;
        } catch (Exception e) {
            System.err.println("[ChartEditor] Failed to preload chart: " + path);
            e.printStackTrace();
            return null;
        }
    }

    private StagePreset getSelectedPreset() {
        return (StagePreset) stageCombo.getSelectedItem();
    }

    private Difficulty getSelectedDifficulty() {
        return (Difficulty) difficultyCombo.getSelectedItem();
    }

    private double beatToSeconds(double beat, double bpm, double offsetSeconds) {
        return beat * (60.0 / bpm) + offsetSeconds;
    }

    private double secondsToBeat(double seconds, double bpm, double offsetSeconds) {
        return (seconds - offsetSeconds) / (60.0 / bpm);
    }

    private double getClipLengthSeconds() {
        if (audioClip == null) {
            return 0.0;
        }
        return audioClip.getMicrosecondLength() / 1_000_000.0;
    }

    private double parseBpm() {
        try {
            double bpm = Double.parseDouble(bpmField.getText().trim());
            return bpm > 0 ? bpm : 120.0;
        } catch (Exception e) {
            return 120.0;
        }
    }

    private double parseOffsetSeconds() {
        try {
            return Double.parseDouble(offsetField.getText().trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double parseStartBeat() {
        try {
            return Double.parseDouble(startBeatField.getText().trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String formatDouble(double value) {
        return String.format("%.3f", value);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChartEditor().setVisible(true));
    }

    private static class StagePreset {
        private final Stage stage;
        private final EnumMap<Difficulty, Boolean> availableDifficulties = new EnumMap<>(Difficulty.class);

        private StagePreset(Stage stage) {
            this.stage = stage;
        }

        private void addAvailableDifficulty(Difficulty difficulty) {
            availableDifficulties.put(difficulty, true);
        }

        private boolean hasDifficulty(Difficulty difficulty) {
            return availableDifficulties.getOrDefault(difficulty, false);
        }

        private boolean hasAnyDifficulty() {
            return !availableDifficulties.isEmpty();
        }

        @Override
        public String toString() {
            return stage.getLevelName();
        }
    }
}

class LanePanel extends JPanel {

    private final ArrayList<NoteData> notes = new ArrayList<>();

    private final int lanes = 4;
    private final int laneWidth = 100;
    private final int noteWidth = 60;
    private final int noteHeight = 12;
    private final int baseX = 100;

    private double pixelsPerBeat = 64.0;
    private double gridInterval = 0.25;

    private double scrollOffsetBeats = 0.0;
    private final double wheelScrollBeats = 1.0;

    private double playbackBeat = 0.0;
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
            scrollOffsetBeats += e.getPreciseWheelRotation() * wheelScrollBeats;

            if (scrollOffsetBeats < 0) {
                scrollOffsetBeats = 0;
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

        double rawBeat = screenYToBeat(e.getY());
        double snappedBeat = Math.round(rawBeat / gridInterval) * gridInterval;

        if (snappedBeat < 0) {
            snappedBeat = 0;
        }

        NoteData existing = findNote(lane, snappedBeat);

        if (existing != null) {
            notes.remove(existing);
        } else {
            notes.add(new NoteData(lane, snappedBeat));
            sortNotes();
        }

        repaint();
    }

    private NoteData findNote(int lane, double beat) {
        final double epsilon = 0.0001;

        for (NoteData note : notes) {
            if (note.lane == lane && Math.abs(note.beat - beat) < epsilon) {
                return note;
            }
        }

        return null;
    }

    private void sortNotes() {
        notes.sort(Comparator.comparingDouble((NoteData n) -> n.beat).thenComparingInt(n -> n.lane));
    }

    private double screenYToBeat(int y) {
        return scrollOffsetBeats + (y / pixelsPerBeat);
    }

    private int beatToScreenY(double beat) {
        return (int) Math.round((beat - scrollOffsetBeats) * pixelsPerBeat);
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

    public void clearNotes() {
        notes.clear();
        repaint();
    }

    public ArrayList<String> exportChartLines() {
        ArrayList<String> lines = new ArrayList<>();
        sortNotes();

        for (NoteData note : notes) {
            String laneText = switch (note.lane) {
                case 0 -> "D";
                case 1 -> "F";
                case 2 -> "J";
                case 3 -> "K";
                default -> throw new IllegalStateException("Unexpected lane: " + note.lane);
            };

            lines.add(String.format("%.3f %s", note.beat, laneText));
        }

        return lines;
    }

    public void loadChartLines(List<String> lines) {
        notes.clear();

        for (String line : lines) {
            if (line == null) {
                continue;
            }

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

            notes.add(new NoteData(lane, beat));
        }

        sortNotes();
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

        double startBeat = scrollOffsetBeats;
        double endBeat = scrollOffsetBeats + (panelHeight / pixelsPerBeat);

        double firstGrid = Math.floor(startBeat / gridInterval) * gridInterval;

        g.setFont(new Font("Arial", Font.PLAIN, 12));

        for (double beat = firstGrid; beat <= endBeat + gridInterval; beat += gridInterval) {
            int y = beatToScreenY(beat);

            boolean major = Math.abs(beat - Math.round(beat)) < 0.0001;

            if (major) {
                g.setColor(new Color(120, 120, 120));
            } else {
                g.setColor(new Color(75, 75, 75));
            }

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

        int y = beatToScreenY(playbackBeat);

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
        g.setColor(Color.CYAN);

        for (NoteData note : notes) {
            int y = beatToScreenY(note.beat) - noteHeight / 2;

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

        g.drawString(String.format("Scroll: %.3f beat", scrollOffsetBeats), 10, 20);
        g.drawString(String.format("Grid: %.3f beat", gridInterval), 10, 40);
        g.drawString(String.format("Cursor: %.3f beat", playbackBeat), 10, 60);
        g.drawString("Left Click: Add/Remove Note", 10, 95);
        g.drawString("Mouse Wheel: Scroll", 10, 115);
    }

    public void saveChart(File file) {
        try {
            ArrayList<String> lines = exportChartLines();
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "저장 실패: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadChart(File file) {
        try {
            loadChartLines(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "불러오기 실패: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}

class NoteData {
    int lane;
    double beat;

    public NoteData(int lane, double beat) {
        this.lane = lane;
        this.beat = beat;
    }
}