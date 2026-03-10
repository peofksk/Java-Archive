package stage;

import java.util.ArrayList;
import java.util.List;

public class StageManager {

	private final List<Stage> stages = new ArrayList<>();
	private int currentIndex = 0;

	public StageManager() {
		loadStages();
	}

	private void loadStages() {

		addStage("unwelcomeSchool");
		addStage("afterSchoolDessert");
		addStage("comingSoon");

	}

	private void addStage(String name) {

		stages.add(new Stage(name, "title_" + name, "/sample_" + name + ".wav", "/" + name + ".wav",
				"stage_" + name + "_bg", "note_" + name + "_", null));
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