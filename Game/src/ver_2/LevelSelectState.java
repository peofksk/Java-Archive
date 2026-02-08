package ver_2;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;

public class LevelSelectState implements GameState {

    private final JAVA_Archive game;

    private Image background, arrowLeft, arrowRight;
    private Image[] titles;
    private Music[] samples;

    private int index = 0;

    public LevelSelectState(JAVA_Archive game) {
        this.game = game;
    }

    @Override
    public void enter() {
        AssetManager am = AssetManager.getInstance();

        background = am.getImage("selection_bg");
        arrowLeft = am.getImage("arrow_left");
        arrowRight = am.getImage("arrow_right");

        titles = new Image[] {
            am.getImage("title_unwelcome"),
            am.getImage("title_after"),
            am.getImage("title_coming")
        };

        samples = new Music[] {
            new Music("sample_unwelcomeSchool.mp3"),
            new Music("sample_afterSchoolDessert.mp3"),
            new Music("sample_comingSoon.mp3")
        };

        samples[index].play();
    }

    @Override
    public void update() {}

    @Override
    public void render(Graphics2D g) {
        g.drawImage(background, 0, 0, null);
        g.drawImage(titles[index], 312, 80, null);

        if (index > 0)
            g.drawImage(arrowLeft, 77, 225, null);
        if (index < titles.length - 1)
            g.drawImage(arrowRight, 713, 225, null);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT && index > 0) {
            change(index - 1);
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT && index < titles.length - 1) {
            change(index + 1);
        }
    }

    private void change(int next) {
        samples[index].stop();
        index = next;
        samples[index].play();
    }

    @Override
    public void exit() {
        samples[index].stop();
    }
}
