package ver_2;

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
        

        am.loadImage("menu_bar", "/menuBar.png");
        am.loadImage("game_title", "/gameTitle.png");
        am.loadImage("press_enter", "/pressEnter.png");
        am.loadImage("arrow_left", "/arrowLeft.png");
        am.loadImage("arrow_right", "/arrowRight.png");
        am.loadImage("corner", "/corner.png");

        am.loadImage("exit", "/exitButton.png");
        am.loadImage("exit_pressed", "/exitButtonPressed.png");

        am.loadImage("title_unwelcome", "/Title_unwelcomeSchool.png");
        am.loadImage("title_after", "/Title_afterSchoolDessert.png");
        am.loadImage("title_coming", "/Title_comingSoon.png");
        
        am.loadText("map_unwelcome_easy",
                "/unwelcomeSchool_Easy.txt");

        am.loadText("map_unwelcome_hard",
                "/unwelcomeSchool_Hard.txt");

        am.loadText("map_after_hard",
                "/afterSchoolDessert_Hard.txt");
    }
}
