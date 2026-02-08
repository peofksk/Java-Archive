package ver_2;

import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ActionMap;
import javax.swing.InputMap;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

public class GamePanel extends JPanel {

    private final JAVA_Archive game;

    public GamePanel(JAVA_Archive game) {
        this.game = game;
        setFocusable(true);
        setupKeyBindings();
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ENTER");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "LEFT");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "RIGHT");

        am.put("ENTER", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispatch(KeyEvent.VK_ENTER);
            }
        });

        am.put("LEFT", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispatch(KeyEvent.VK_LEFT);
            }
        });

        am.put("RIGHT", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispatch(KeyEvent.VK_RIGHT);
            }
        });
    }

    private void dispatch(int keyCode) {
        GameState state = game.getCurrentState();
        if (state != null) {
            state.keyPressed(new KeyEvent(
                this,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                0,
                keyCode,
                KeyEvent.CHAR_UNDEFINED
            ));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        GameState state = game.getCurrentState();
        if (state != null) {
            state.render((Graphics2D) g);
        }
    }
}
