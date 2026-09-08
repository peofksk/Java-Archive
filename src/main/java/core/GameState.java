package core;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public interface GameState {

    void enter();

    void update(double deltaTime);

    void render(Graphics2D g);

    default void keyPressed(KeyEvent e) {
    }

    default void keyReleased(KeyEvent e) {
    }

    default void mousePressed(MouseEvent e) {
    }

    default void mouseReleased(MouseEvent e) {
    }

    default void mouseMoved(MouseEvent e) {
    }

    default void mouseDragged(MouseEvent e) {
    }

    default void mouseWheelMoved(MouseWheelEvent e) {
    }

    void exit();
}