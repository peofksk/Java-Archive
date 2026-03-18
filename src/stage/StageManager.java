package stage;

import java.util.ArrayList;
import java.util.List;

public class StageManager {

	private final List<Stage> stages = new ArrayList<>();
	private int currentIndex = 0;

	private final CorrectionConfig correctionConfig;

	public StageManager() {
		loadStages();
		correctionConfig = loadCorrectionConfig();
	}

	private void loadStages() {
		addGameStage("unwelcomeSchool", 180.0, 0.0);
		addGameStage("afterSchoolDessert", 160.0, 1.23);
		addGameStage("comingSoon", 120.0, 0.0);
	}

	private void addGameStage(String name, double musicBPM, double musicOffsetSeconds) {
		stages.add(new Stage(name, "title_" + name, "/sample_" + name + ".wav", "/" + name + ".wav",
				"stage_" + name + "_bg", "note_" + name + "_", musicBPM, musicOffsetSeconds));
	}

	private CorrectionConfig loadCorrectionConfig() {
		return new CorrectionConfig("correction", "/correction.wav", "correction_bg", "note_correction", 112.0, 0.0);
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

	public CorrectionConfig getCurrentCorrectionConfig() {
		return correctionConfig;
	}

	public CorrectionConfig getCurrentStageBasedCorrectionConfig() {
		Stage stage = getCurrentStage();

		return new CorrectionConfig(stage.getLevelName(), stage.getMusicPath(), stage.getBackgroundImageKey(),
				stage.getNoteFilePath(), stage.getMusicBPM(), stage.getMusicOffsetSeconds());
	}
}