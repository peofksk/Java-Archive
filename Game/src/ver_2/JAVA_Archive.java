package ver_2;

import javax.swing.JFrame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class JAVA_Archive extends JFrame implements KeyListener {

    private GameState currentState;

    public JAVA_Archive() {
        setTitle("JAVA_Archive");
        setSize(1024, 576);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        addKeyListener(this);
        setFocusable(true);
        requestFocus();

        changeState(new IntroState(this));
        setVisible(true);
    }

    public void changeState(GameState next) {
        if (currentState != null) currentState.exit();
        currentState = next;
        currentState.enter();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (currentState != null) {
            currentState.render(g2);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (currentState != null) {
            currentState.keyPressed(e);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (currentState != null) {
            currentState.keyReleased(e);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
