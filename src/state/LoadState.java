package state;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

import core.GameContext;
import core.GameState;
import core.LoadTask;

public class LoadState implements GameState {

    private final GameContext context;
    private final String message;
    private final LoadTask loadTask;
    private final Supplier<GameState> nextStateSupplier;

    private volatile boolean started = false;
    private volatile boolean finished = false;
    private volatile Exception error = null;

    public LoadState(GameContext context, String message, LoadTask loadTask, Supplier<GameState> nextStateSupplier) {
        this.context = context;
        this.message = message;
        this.loadTask = loadTask;
        this.nextStateSupplier = nextStateSupplier;
    }

    @Override
    public void enter() {
        if (started) {
            return;
        }

        started = true;

        Thread loader = new Thread(() -> {
            try {
                loadTask.run();
                finished = true;
            } catch (Exception e) {
                error = e;
            }
        });

        loader.setDaemon(true);
        loader.start();
    }

    @Override
    public void exit() {
    }

    @Override
    public void update(double deltaTime) {
        if (error != null) {
            error.printStackTrace();
            return;
        }

        if (finished) {
            context.changeState(nextStateSupplier.get());
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1024, 576);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString(message, 380, 270);

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Please wait...", 430, 315);

        if (error != null) {
            g.setColor(Color.RED);
            g.drawString("Load Failed", 430, 360);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}