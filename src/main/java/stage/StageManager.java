package stage;

import java.util.ArrayList;
import java.util.List;

public class StageManager {

	private final List<Stage> stages = new ArrayList<>();
	private int currentIndex = 0;

	private final CalibrationConfig calibrationConfig;

	public StageManager() {
		loadStages();
		calibrationConfig = loadCorrectionConfig();
	}

	private void loadStages() {
		addGameStage("unwelcomeSchool", 180.0, 0.0);
		addGameStage("afterSchoolDessert", 160.0, 1.233);
		addGameStage("operationDotabata", 140.0, 0.176);
		addGameStage("comingSoon", 120.0, 0.0);
	}

	private void addGameStage(String name, double musicBPM, double musicOffsetSeconds) {
		stages.add(new Stage(name, "title_" + name, "/audio/bgm/" + name + ".wav", "/audio/level/" + name + ".wav",
				"stage_" + name + "_bg", "note_" + name + "_", musicBPM, musicOffsetSeconds));
	}

	private CalibrationConfig loadCorrectionConfig() {
		return new CalibrationConfig("calibration", "/audio/bgm/calibration.wav", "calibration_bg", 112.0, 0.0);
	}

	public Stage getCurrentStage() {
		return stages.get(currentIndex);
	}

	public int getStageSize() {
		return stages.size();
	}

	public boolean hasNext() {
		return currentIndex < stages.size() - 1;
	}

	public boolean hasPrev() {
		return currentIndex > 0;
	}

	public void next() {
		if (hasNext()) {
			currentIndex++;
		}
	}

	public void prev() {
		if (hasPrev()) {
			currentIndex--;
		}
	}

	public CalibrationConfig getCurrentCorrectionConfig() {
		return calibrationConfig;
	}

	public CalibrationConfig getCurrentStageBasedCorrectionConfig() {
		Stage stage = getCurrentStage();

		return new CalibrationConfig(stage.getLevelName(), stage.getMusicPath(), stage.getBackgroundImageKey(),
				stage.getMusicBPM(), stage.getMusicOffsetSeconds());
	}
}