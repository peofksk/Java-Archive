package asset;

public class Init {

    public static void loadAssets() {

        AssetManager am = AssetManager.getInstance();

        am.loadImage("intro_bg", "/introBackground.jpg");
        am.loadImage("selection_bg", "/selectionBackground.png");
        am.loadImage("option_bg", "/optionBackground.jpg");
        am.loadImage("correction_bg", "/correctionBackground.png");
        am.loadImage("result_bg", "/resultBackground.png");

        am.loadImage("stage_unwelcomeSchool_bg", "/unwelcomeSchoolBackground.png");
        am.loadImage("stage_afterSchoolDessert_bg", "/afterSchoolDessertBackground.png");
        am.loadImage("stage_comingSoon_bg", "/test.png");

        am.loadImage("title_unwelcomeSchool", "/Title_unwelcomeSchool.png");
        am.loadImage("title_afterSchoolDessert", "/Title_afterSchoolDessert.png");
        am.loadImage("title_comingSoon", "/Title_comingSoon.png");

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

        am.loadText("note_correction", "/correction.txt");
        am.loadText("note_unwelcomeSchool_easy", "/unwelcomeSchool_Easy.txt");
        am.loadText("note_unwelcomeSchool_hard", "/unwelcomeSchool_Hard.txt");
        am.loadText("note_unwelcomeSchool_extreme", "/unwelcomeSchool_Extreme.txt");
        am.loadText("note_afterSchoolDessert_easy", "/afterSchoolDessert_Easy.txt");
        am.loadText("note_afterSchoolDessert_hard", "/afterSchoolDessert_Hard.txt");
        // am.loadText("note_afterSchoolDessert_extreme", "/afterSchoolDessert_Extreme.txt");
    }
}