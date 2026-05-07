package asset;

import stage.Difficulty;

public class Init {

    public static void loadAssets() {
        AssetManager am = AssetManager.getInstance();

        loadImages(am);
        loadTexts(am);
    }

    private static void loadImages(AssetManager am) {
        am.loadImage("intro_bg", "/introBackground.jpg");
        am.loadImage("selection_bg", "/selectionBackground.png");
        am.loadImage("option_bg", "/optionBackground.jpg");
        am.loadImage("correction_bg", "/correctionBackground.png");
        am.loadImage("result_bg", "/resultBackground.png");

        am.loadImage("stage_unwelcomeSchool_bg", "/unwelcomeSchoolBackground.png");
        am.loadImage("stage_afterSchoolDessert_bg", "/afterSchoolDessertBackground.png");
        am.loadImage("stage_operationDotabata_bg", "/operationDotabataBackground.png");
        am.loadImage("stage_comingSoon_bg", "/test.png");

		/*
		am.loadImage("stage_unwelcomeSchool_bg", "/testGameBackground.png");
		am.loadImage("stage_afterSchoolDessert_bg", "/testGameBackground.png");
		am.loadImage("stage_operationDotabata_bg", "/testGameBackground.png");
		am.loadImage("stage_comingSoon_bg", "/testGameBackground.png");
		*/

        am.loadImage("title_unwelcomeSchool", "/title_unwelcomeSchool.png");
        am.loadImage("title_afterSchoolDessert", "/title_afterSchoolDessert.png");
        am.loadImage("title_operationDotabata", "/title_operationDotabata.png");
        am.loadImage("title_comingSoon", "/title_comingSoon.png");

        am.loadImage("menu_bar", "/menuBar.png");
        am.loadImage("game_title", "/gameTitle.png");
        am.loadImage("press_enter", "/pressEnter.png");
        am.loadImage("arrow_left", "/arrowLeft.png");
        am.loadImage("arrow_right", "/arrowRight.png");
        am.loadImage("corner", "/corner.png");

        am.loadImage("exit", "/exitButton.png");
        am.loadImage("exit_pressed", "/exitButtonPressed.png");

        am.loadImage("lane", "/lane.png");
        am.loadImage("judgement_line", "/judgement_line.png");

        for (int i = 0; i <= 11; i++) {
            am.loadImage("note_" + i, "/note_" + i + ".png");
        }

        am.loadImage("note_image", "/note_0.png");
    }

    private static void loadTexts(AssetManager am) {
        am.loadText("note_correction", "/correction.txt");

        loadChartsForStage(am, "unwelcomeSchool");
        loadChartsForStage(am, "afterSchoolDessert");
        loadChartsForStage(am, "operationDotabata");
    }

    private static void loadChartsForStage(AssetManager am, String levelName) {
        for (Difficulty difficulty : Difficulty.values()) {
            loadChart(am, levelName, difficulty, 4);
            loadChart(am, levelName, difficulty, 6);
        }
    }

    private static void loadChart(AssetManager am, String levelName, Difficulty difficulty, int keyCount) {
        String key = buildChartKey(levelName, difficulty, keyCount);
        String path = "/" + key + ".txt";

        am.loadText(key, path);
    }

    private static String buildChartKey(String levelName, Difficulty difficulty, int keyCount) {
        return levelName + "_" + difficulty.name() + "_" + keyCount + "K";
    }
}