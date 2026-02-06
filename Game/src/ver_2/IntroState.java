package ver_2;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;

public class IntroState implements GameState {

    private JAVA_Archive game;
    private Image background;
    private Image gameTitle;
    private Music introMusic;

    public IntroState(JAVA_Archive game) {
        this.game = game;
    }

    @Override
    public void enter() {
        background = AssetManager.getInstance().getImage("intro_bg");
        gameTitle = AssetManager.getInstance().getImage("game_title");
        introMusic = AssetManager.getInstance().getMusic("intro_bgm");
    }

    @Override
    public void update() {
    	introMusic.run();
    }

    @Override
    public void render(Graphics2D g) {
        if (background != null) {
            g.drawImage(background, 0, 0, null);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 1024, 576);
        }
        if (gameTitle != null) {
        	g.drawImage(gameTitle, 172, 121, null);
        }
        
    }

    @Override
    public void exit() {
    }
}
