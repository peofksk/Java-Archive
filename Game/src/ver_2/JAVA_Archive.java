package ver_2;

import javax.swing.JFrame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class JAVA_Archive extends JFrame implements KeyListener {

    private GameState currentState;
    private GamePanel panel;

    public JAVA_Archive() {
        setTitle("JAVA_Archive");
        setSize(1024, 576);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        panel = new GamePanel(this);
        add(panel);

        addKeyListener(this);

        setVisible(true);
        panel.requestFocus();

        changeState(new IntroState(this));

        startGameLoop();
    }

    private void startGameLoop() {
        new Thread(() -> {
            while (true) {
                if (currentState != null) currentState.update();
                panel.repaint();
                try {
                    Thread.sleep(16); // 60fps
                } catch (InterruptedException e) {}
            }
        }).start();
    }

    public void changeState(GameState next) {
        if (currentState != null) currentState.exit();
        currentState = next;
        if (currentState != null) currentState.enter();
    }

    public GameState getCurrentState() {
        return currentState;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (currentState != null) currentState.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (currentState != null) currentState.keyReleased(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
