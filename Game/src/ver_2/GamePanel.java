package ver_2;

import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.AbstractAction;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
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

        bindKey(im, am, "ENTER", KeyEvent.VK_ENTER);
        bindKey(im, am, "LEFT", KeyEvent.VK_LEFT);
        bindKey(im, am, "RIGHT", KeyEvent.VK_RIGHT);
        bindKey(im, am, "ESCAPE", KeyEvent.VK_ESCAPE);
    }

    private void bindKey(InputMap im, ActionMap am, String name, int keyCode) {

        im.put(KeyStroke.getKeyStroke("pressed " + name), name + "_PRESSED");
        im.put(KeyStroke.getKeyStroke("released " + name), name + "_RELEASED");

        am.put(name + "_PRESSED", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispatch(keyCode, KeyEvent.KEY_PRESSED);
            }
        });

        am.put(name + "_RELEASED", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispatch(keyCode, KeyEvent.KEY_RELEASED);
            }
        });
    }

    private void dispatch(int keyCode, int type) {
        GameState state = game.getCurrentState();
        if (state == null) return;

        KeyEvent e = new KeyEvent(
            this,
            type,
            System.currentTimeMillis(),
            0,
            keyCode,
            KeyEvent.CHAR_UNDEFINED
        );

        if (type == KeyEvent.KEY_PRESSED) {
            state.keyPressed(e);
        } else if (type == KeyEvent.KEY_RELEASED) {
            state.keyReleased(e);
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
