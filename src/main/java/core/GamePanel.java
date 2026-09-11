package core;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;

public class GamePanel extends JPanel
        implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private static final long serialVersionUID = 1L;

    private final GameContext context;

    public GamePanel(GameContext context) {
        this.context = context;

        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
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
        if (context.getCurrentState() != null) {
            context.getCurrentState().keyPressed(e);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (context.getCurrentState() != null) {
            context.getCurrentState().keyReleased(e);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();

        if (context.getCurrentState() != null) {
            context.getCurrentState().mousePressed(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (context.getCurrentState() != null) {
            context.getCurrentState().mouseReleased(e);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (context.getCurrentState() != null) {
            context.getCurrentState().mouseMoved(e);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (context.getCurrentState() != null) {
            context.getCurrentState().mouseDragged(e);
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (context.getCurrentState() != null) {
            context.getCurrentState().mouseWheelMoved(e);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (context.getCurrentState() != null) {
            context.getCurrentState().mouseMoved(e);
        }
    }
}