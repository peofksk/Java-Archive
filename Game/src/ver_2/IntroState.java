package ver_2;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

public class IntroState implements GameState {

    private final JAVA_Archive game;
    private Image background, gameTitle, pressEnter;
    private Music introMusic;

    public IntroState(JAVA_Archive game) {
        this.game = game;
    }

    @Override
    public void enter() {
        background = AssetManager.getInstance().getImage("intro_bg");
        gameTitle = AssetManager.getInstance().getImage("game_title");
        pressEnter = AssetManager.getInstance().getImage("press_enter");
        introMusic = new Music("introMusic.mp3", true);
    	introMusic.start();
    }

    @Override
    public void update() {
    	
    }

    @Override
    public void render(Graphics2D g) {
    	g.drawImage(background, 0, 0, null);    
        g.drawImage(gameTitle, 172, 121, null);
        g.drawImage(pressEnter, 357, 450, null);
        
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
    	if (e.getKeyCode() == KeyEvent.VK_ENTER) {
    		game.changeState(new LevelSelectState(game));
    	}
    }

    @Override
    public void exit() {
    	introMusic.close();
    }
}
