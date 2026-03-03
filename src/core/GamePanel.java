package core;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPanel;

public class GamePanel extends JPanel implements KeyListener {

	private static final long serialVersionUID = 1L;
	
	private final GameContext context;

    public GamePanel(GameContext context) {
        this.context = context;
        setFocusable(true);
        addKeyListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (context.getCurrentState() != null) {
            context.getCurrentState().render((Graphics2D) g);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (context.getCurrentState() != null)
            context.getCurrentState().keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (context.getCurrentState() != null)
            context.getCurrentState().keyReleased(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}