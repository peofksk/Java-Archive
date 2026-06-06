package asset;

import stage.Difficulty;
import state.gameplay.KeyMode;

public class Init {
    private static final String[] STAGE_NAMES = {
            "unwelcomeSchool",
            "afterSchoolDessert",
            "operationDotabata"
    };

    public static void loadAssets() {
        AssetManager am = AssetManager.getInstance();

        loadImages(am);
        loadTexts(am);
    }

    private static void loadImages(AssetManager am) {
        loadBackgroundImages(am);
        loadTitleImages(am);
        loadUiImages(am);
        loadNoteImages(am);
    }

    private static void loadBackgroundImages(AssetManager am) {
        am.loadImage("intro_bg", "/image/background/introBackground.jpg");
        am.loadImage("level_selection_bg", "/image/background/levelSelectionBackground.png");
        am.loadImage("option_bg", "/image/background/optionBackground.jpg");
        am.loadImage("calibration_bg", "/image/background/calibrationBackground.png");
        am.loadImage("result_bg", "/image/background/resultBackground.png");

        am.loadImage("stage_unwelcomeSchool_bg", "/image/background/unwelcomeSchoolBackground.png");
        am.loadImage("stage_afterSchoolDessert_bg", "/image/background/afterSchoolDessertBackground.png");
        am.loadImage("stage_operationDotabata_bg", "/image/background/operationDotabataBackground.png");
        am.loadImage("stage_test_bg", "/image/background/testGameBackground.png");
    }

    private static void loadTitleImages(AssetManager am) {
        am.loadImage("title_unwelcomeSchool", "/image/title/unwelcomeSchool.png");
        am.loadImage("title_afterSchoolDessert", "/image/title/afterSchoolDessert.png");
        am.loadImage("title_operationDotabata", "/image/title/operationDotabata.png");
        am.loadImage("title_comingSoon", "/image/title/comingSoon.png");

        am.loadImage("game_title", "/image/title/gameTitle.png");
        am.loadImage("press_enter", "/image/title/pressEnter.png");
    }

    private static void loadUiImages(AssetManager am) {
        am.loadImage("menu_bar", "/image/ui/menuBar.png");
        am.loadImage("arrow_left", "/image/ui/arrowLeft.png");
        am.loadImage("arrow_right", "/image/ui/arrowRight.png");
        am.loadImage("corner", "/image/ui/corner.png");
        am.loadImage("exit", "/image/ui/exitButton.png");
        am.loadImage("exit_pressed", "/image/ui/exitButtonPressed.png");
        am.loadImage("lane", "/image/ui/lane.png");
        am.loadImage("judgement_line", "/image/ui/judgement_line.png");
    }

    private static void loadNoteImages(AssetManager am) {
        for (int i = 0; i <= 11; i++) {
            am.loadImage("note_" + i, "/image/note/note_" + i + ".png");
        }

        am.loadImage("note_image", "/image/note/note_0.png");
    }

    private static void loadTexts(AssetManager am) {
        loadStageCharts(am);
    }

    private static void loadStageCharts(AssetManager am) {
        for (String stageName : STAGE_NAMES) {
            loadChartsForStage(am, stageName);
        }
    }

    private static void loadChartsForStage(AssetManager am, String stageName) {
        for (Difficulty difficulty : Difficulty.values()) {
            for (KeyMode keyMode : KeyMode.values()) {
                loadChart(am, stageName, difficulty, keyMode);
            }
        }
    }

    private static void loadChart(
            AssetManager am,
            String stageName,
            Difficulty difficulty,
            KeyMode keyMode
    ) {
        String key = buildChartKey(stageName, difficulty, keyMode);
        String path = buildChartPath(stageName, difficulty, keyMode);

        am.loadText(key, path);
    }

    public static String buildChartKey(
            String stageName,
            Difficulty difficulty,
            KeyMode keyMode
    ) {
        return buildChartKey(stageName, difficulty, keyMode.getKeyCount());
    }

    public static String buildChartKey(
            String stageName,
            Difficulty difficulty,
            int keyCount
    ) {
        return "note_" + stageName + "_" + difficulty.name().toLowerCase() + "_" + keyCount + "K";
    }

    public static String buildChartPath(
            String stageName,
            Difficulty difficulty,
            KeyMode keyMode
    ) {
        return buildChartPath(stageName, difficulty, keyMode.getKeyCount());
    }

    public static String buildChartPath(
            String stageName,
            Difficulty difficulty,
            int keyCount
    ) {
        return "/chart/" + stageName + "/" + difficulty.name() + "_" + keyCount + "K.txt";
    }
}