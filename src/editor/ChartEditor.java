package editor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

import stage.Difficulty;
import stage.Stage;
import stage.StageManager;
import state.gameplay.KeyMode;

public class ChartEditor extends JFrame {

	private final LanePanel lanePanel;
	private final Timer playbackTimer;

	private final Map<String, StagePreset> presetMap = new LinkedHashMap<>();
	private final Map<String, byte[]> preloadedMusicBytes = new LinkedHashMap<>();
	private final Map<String, ArrayList<String>> preloadedCharts = new LinkedHashMap<>();

	private JComboBox<StagePreset> stageCombo;
	private JComboBox<Difficulty> difficultyCombo;
	private JComboBox<KeyMode> keyModeCombo;

	private JTextField bpmField;
	private JTextField offsetField;
	private JTextField startBeatField;
	private JLabel musicLabel;

	private Clip audioClip;
	private double playbackStartBeat = 0.0;
	private boolean updatingPresetSelectors = false;

	public ChartEditor() {
		setTitle("Rhythm Chart Editor");
		setSize(1180, 860);
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

		JPanel controlsWrapper = new JPanel(new BorderLayout());
		JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));

		JButton loadChartButton = new JButton("Load Chart File");
		JButton saveChartButton = new JButton("Save Chart As");
		JButton overwritePresetButton = new JButton("Overwrite Preset");
		JButton clearButton = new JButton("Clear");

		stageCombo = new JComboBox<>();
		difficultyCombo = new JComboBox<>();
		keyModeCombo = new JComboBox<>();

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
		JTextField speedField = new JTextField("100", 5);

		topRow.add(loadChartButton);
		topRow.add(saveChartButton);
		topRow.add(overwritePresetButton);
		topRow.add(clearButton);
		topRow.add(Box.createHorizontalStrut(10));

		topRow.add(new JLabel("Stage:"));
		topRow.add(stageCombo);
		topRow.add(new JLabel("Difficulty:"));
		topRow.add(difficultyCombo);
		topRow.add(new JLabel("Key:"));
		topRow.add(keyModeCombo);
		topRow.add(reloadPresetButton);
		topRow.add(Box.createHorizontalStrut(10));

		topRow.add(playButton);
		topRow.add(stopButton);
		topRow.add(exitButton);

		bottomRow.add(bpmLabel);
		bottomRow.add(bpmField);
		bottomRow.add(offsetLabel);
		bottomRow.add(offsetField);
		bottomRow.add(startBeatLabel);
		bottomRow.add(startBeatField);
		bottomRow.add(useScrollButton);
		bottomRow.add(Box.createHorizontalStrut(10));

		bottomRow.add(gridLabel);
		bottomRow.add(gridCombo);
		bottomRow.add(Box.createHorizontalStrut(10));

		bottomRow.add(speedLabel);
		bottomRow.add(speedField);

		musicLabel = new JLabel("No preset loaded");
		musicLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 6, 10));

		controlsWrapper.add(topRow, BorderLayout.NORTH);
		controlsWrapper.add(bottomRow, BorderLayout.SOUTH);

		wrapper.add(controlsWrapper, BorderLayout.NORTH);
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

			Difficulty selectedDifficulty = getSelectedDifficulty();
			refreshDifficultyCombo(getSelectedPreset(), selectedDifficulty);
			loadSelectedPreset();
		});

		difficultyCombo.addActionListener(e -> {
			if (updatingPresetSelectors) {
				return;
			}

			loadSelectedPreset();
		});

		keyModeCombo.addActionListener(e -> {
			if (updatingPresetSelectors) {
				return;
			}

			KeyMode selectedKeyMode = getSelectedKeyMode();
			if (selectedKeyMode != null) {
				lanePanel.setKeyMode(selectedKeyMode);
			}

			refreshDifficultyCombo(getSelectedPreset(), getSelectedDifficulty());
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

		useScrollButton.addActionListener(
				e -> startBeatField.setText(String.format("%.3f", lanePanel.getScrollOffsetBeats())));

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
				for (KeyMode keyMode : KeyMode.values()) {
					String chartResourcePath = buildChartResourcePath(stage, difficulty, keyMode);
					ArrayList<String> chartLines = readTextResource(chartResourcePath);

					if (chartLines != null) {
						String chartCacheKey = buildChartCacheKey(stage, difficulty, keyMode);
						preloadedCharts.put(chartCacheKey, chartLines);
						preset.addAvailableChart(difficulty, keyMode);
					}
				}
			}

			presetMap.put(stage.getLevelName(), preset);
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
		difficultyCombo.removeAllItems();
		keyModeCombo.removeAllItems();

		for (StagePreset preset : presetMap.values()) {
			stageCombo.addItem(preset);
		}

		for (KeyMode keyMode : KeyMode.values()) {
			keyModeCombo.addItem(keyMode);
		}

		updatingPresetSelectors = false;

		if (stageCombo.getItemCount() == 0) {
			musicLabel.setText("프리로드 가능한 레벨이 없습니다.");
			difficultyCombo.setEnabled(false);
			keyModeCombo.setEnabled(false);
			return;
		}

		stageCombo.setSelectedIndex(0);
		keyModeCombo.setSelectedItem(KeyMode.KEY_4);

		lanePanel.setKeyMode(KeyMode.KEY_4);
		refreshDifficultyCombo(getSelectedPreset(), Difficulty.Easy);
		loadSelectedPreset();
	}

	private void refreshDifficultyCombo(StagePreset preset, Difficulty preferredDifficulty) {
		updatingPresetSelectors = true;
		difficultyCombo.removeAllItems();

		if (preset != null) {
			for (Difficulty difficulty : Difficulty.values()) {
				difficultyCombo.addItem(difficulty);
			}
		}

		if (difficultyCombo.getItemCount() > 0) {
			Difficulty toSelect = preferredDifficulty;

			if (toSelect == null) {
				toSelect = Difficulty.Easy;
			}

			boolean found = false;
			for (int i = 0; i < difficultyCombo.getItemCount(); i++) {
				if (difficultyCombo.getItemAt(i) == toSelect) {
					found = true;
					break;
				}
			}

			if (!found) {
				toSelect = difficultyCombo.getItemAt(0);
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
		KeyMode keyMode = getSelectedKeyMode();

		if (preset == null || difficulty == null || keyMode == null) {
			return;
		}

		stopAudio();
		closeAudio();

		lanePanel.setKeyMode(keyMode);

		bpmField.setText(formatDouble(preset.stage.getMusicBPM()));
		offsetField.setText(formatDouble(preset.stage.getMusicOffsetSeconds()));

		ArrayList<String> chartLines = preloadedCharts.get(buildChartCacheKey(preset.stage, difficulty, keyMode));
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
				JOptionPane.showMessageDialog(this, "프리로드된 음악 열기 실패: " + e.getMessage(), "Error",
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
		KeyMode keyMode = getSelectedKeyMode();

		if (preset == null || difficulty == null || keyMode == null) {
			JOptionPane.showMessageDialog(this, "선택된 프리셋이 없습니다.", "Info", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		String fileName = buildChartFileName(preset.stage, difficulty, keyMode);
		Path targetPath = resolveWritableChartPath(preset.stage, difficulty, keyMode);

		if (targetPath == null) {
			JOptionPane.showMessageDialog(this,
					"원본 채보 파일 경로를 찾지 못했습니다.\n" + "아래 이름으로 asset 폴더에 파일을 두고 다시 시도하세요:\n" + fileName, "Error",
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

			String chartCacheKey = buildChartCacheKey(preset.stage, difficulty, keyMode);
			preloadedCharts.put(chartCacheKey, new ArrayList<>(lines));
			preset.addAvailableChart(difficulty, keyMode);

			updatePresetStatusLabel();

			JOptionPane.showMessageDialog(this, "프리셋 원본 채보에 바로 저장했습니다.\n" + targetPath.toAbsolutePath(), "Saved",
					JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "프리셋 덮어쓰기 실패: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private Path resolveWritableChartPath(Stage stage, Difficulty difficulty, KeyMode keyMode) {
		String resourcePath = buildChartResourcePath(stage, difficulty, keyMode);
		String fileName = buildChartFileName(stage, difficulty, keyMode);

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
		try (AudioInputStream audioInputStream = AudioSystem
				.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(audioBytes)))) {
			Clip clip = AudioSystem.getClip();
			clip.open(audioInputStream);
			return clip;
		}
	}

	private void playAudio() {
		if (audioClip == null) {
			JOptionPane.showMessageDialog(this, "선택한 레벨의 음악이 프리로드되지 않았습니다.", "Info", JOptionPane.INFORMATION_MESSAGE);
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
		KeyMode keyMode = getSelectedKeyMode();

		if (preset == null || difficulty == null || keyMode == null) {
			musicLabel.setText("No preset loaded");
			return;
		}

		boolean chartExists = preset.hasChart(difficulty, keyMode);
		String chartStatus = chartExists ? "loaded" : "new/empty";
		String musicStatus = audioClip != null ? String.format("%.3f s", getClipLengthSeconds()) : "missing";

		musicLabel.setText(String.format(
				"Preset: %s / %s / %dK | Chart: %s | BPM: %s | Offset: %s s | Notes: %d | Lanes: %d | Music: %s | Ctrl+S: overwrite",
				preset.stage.getLevelName(), difficulty.name(), keyMode.getKeyCount(), chartStatus,
				formatDouble(parseBpm()), formatDouble(parseOffsetSeconds()), lanePanel.getNoteCount(),
				lanePanel.getLaneCount(), musicStatus));
	}

	private String buildChartResourcePath(Stage stage, Difficulty difficulty, KeyMode keyMode) {
		return "/" + buildChartFileName(stage, difficulty, keyMode);
	}

	private String buildChartFileName(Stage stage, Difficulty difficulty, KeyMode keyMode) {
		return stage.getLevelName() + "_" + difficulty.name() + "_" + keyMode.getKeyCount() + "K.txt";
	}

	private String buildChartCacheKey(Stage stage, Difficulty difficulty, KeyMode keyMode) {
		return stage.getLevelName() + "::" + difficulty.name() + "::" + keyMode.getKeyCount() + "K";
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

	private KeyMode getSelectedKeyMode() {
		return (KeyMode) keyModeCombo.getSelectedItem();
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
		private final Map<KeyMode, EnumMap<Difficulty, Boolean>> availableCharts = new LinkedHashMap<>();

		private StagePreset(Stage stage) {
			this.stage = stage;
		}

		private void addAvailableChart(Difficulty difficulty, KeyMode keyMode) {
			availableCharts.computeIfAbsent(keyMode, unused -> new EnumMap<>(Difficulty.class)).put(difficulty, true);
		}

		private boolean hasChart(Difficulty difficulty, KeyMode keyMode) {
			EnumMap<Difficulty, Boolean> difficulties = availableCharts.get(keyMode);

			if (difficulties == null) {
				return false;
			}

			return difficulties.getOrDefault(difficulty, false);
		}

		@Override
		public String toString() {
			return stage.getLevelName();
		}
	}
}