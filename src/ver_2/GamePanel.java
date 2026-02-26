package ver_2;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class GamePanel extends JPanel implements KeyListener {

    private final JAVA_Archive game;

    public GamePanel(JAVA_Archive game) {
        this.game = game;
        setFocusable(true);
        addKeyListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (game.getCurrentState() != null) {
            game.getCurrentState().render((Graphics2D) g);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (game.getCurrentState() != null)
            game.getCurrentState().keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (game.getCurrentState() != null)
            game.getCurrentState().keyReleased(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}