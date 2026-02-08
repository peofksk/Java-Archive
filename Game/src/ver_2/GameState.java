package ver_2;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

public interface GameState {
    void enter();
    void update();
    void render(Graphics2D g);
    void exit();

    default void keyPressed(KeyEvent e) {}
    default void keyReleased(KeyEvent e) {}
}
