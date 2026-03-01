package ver_2;

import java.util.ArrayList;
import java.util.List;

public class StageManager {

	private final List<Stage> stages = new ArrayList<>();
	private int currentIndex = 0;

	public StageManager() {
		loadStages();
	}

	private void loadStages() {
		stages.add(new Stage("title_unwelcome", "/sample_unwelcomeSchool.wav", "/unwelcomeSchool.wav", "stage_unwelcomeSchool_bg", null));

		stages.add(new Stage("title_after", "/sample_afterSchoolDessert.wav", "/afterSchoolDessert.wav", "stage_afterSchoolDessert_bg", null));

		stages.add(new Stage("title_coming", "/sample_comingSoon.wav", "/comingSoon.wav", "stage_comingSoon_bg", null));
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
		if (currentIndex < stages.size() - 1) {
			currentIndex++;
		}
	}

	public void prev() {
		if (currentIndex > 0) {
			currentIndex--;
		}
	}
}