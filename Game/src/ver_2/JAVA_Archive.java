package ver_2;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JFrame;

public class JAVA_Archive extends JFrame {

    private GameState currentState;

    public JAVA_Archive() {
        setTitle("JAVA_Archive");
        setSize(1024, 576);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        // ★ 실행 시 바로 IntroState로 진입
        changeState(new IntroState(this));
    }

    public void changeState(GameState next) {
        if (currentState != null) {
            currentState.exit();
        }
        currentState = next;
        currentState.enter();
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (currentState != null) {
            Graphics2D g2d = (Graphics2D) g;
            currentState.render(g2d);
        }
    }
}
