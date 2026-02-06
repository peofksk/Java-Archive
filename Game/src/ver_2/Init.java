package ver_2;

public class Init {

    public static void loadAssets() {

        AssetManager am = AssetManager.getInstance();
        
        am.loadImage("intro_bg", "../Asset/introBackground.jpg");
        am.loadImage("selection_bg", "../Asset/selectionBackground.png");
        am.loadImage("option_bg", "../Asset/optionBackground.jpg");
        am.loadImage("correction_bg", "../Asset/correctionBackground.png");
        am.loadImage("result_bg", "../Asset/resultBackground.png");

        am.loadImage("menu_bar", "../Asset/menuBar.png");
        am.loadImage("game_title", "../Asset/gameTitle.png");
        am.loadImage("press_enter", "../Asset/pressEnter.png");
        am.loadImage("arrow_left", "../Asset/arrowLeft.png");
        am.loadImage("arrow_right", "../Asset/arrowRight.png");
        am.loadImage("corner", "../Asset/corner.png");

        am.loadImage("exit", "../Asset/exitButton.png");
        am.loadImage("exit_pressed", "../Asset/exitButtonPressed.png");

        am.loadImage("title_unwelcome", "../Asset/Title_unwelcomeSchool.png");
        am.loadImage("title_after", "../Asset/Title_afterSchoolDessert.png");
        am.loadImage("title_coming", "../Asset/Title_comingSoon.png");

        am.loadMusic("intro_bgm", "introMusic.mp3", true);
        am.loadMusic("option_bgm", "optionMusic.mp3", true);
        am.loadMusic("result_bgm", "resultMusic.mp3", true);
        am.loadMusic("correction_bgm", "correctionMusic.mp3", true);

        am.loadMusic("sample_unwelcome", "sample_unwelcomeSchool.mp3", true);
        am.loadMusic("sample_after", "sample_afterSchoolDessert.mp3", true);

        am.loadText("map_unwelcome_easy",
                "../Asset/unwelcomeSchool_Easy.txt");

        am.loadText("map_unwelcome_hard",
                "../Asset/unwelcomeSchool_Hard.txt");

        am.loadText("map_after_hard",
                "../Asset/afterSchoolDessert_Hard.txt");
    }
}
