package ver_2;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

public class IntroState implements GameState {

    private final JAVA_Archive game;
    private Image background, title, pressEnter;

    public IntroState(JAVA_Archive game) {
        this.game = game;
    }

    @Override
    public void enter() {
        AssetManager am = AssetManager.getInstance();
        background = am.getImage("intro_bg");
        title = am.getImage("game_title");
        pressEnter = am.getImage("press_enter");

        game.getContext().bgm.play("introMusic.mp3", true);
    }

    @Override public void update() {}

    @Override
    public void render(Graphics2D g) {
        g.drawImage(background, 0, 0, null);
        g.drawImage(title, 172, 121, null);
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
        game.getContext().bgm.stop();
    }
}
