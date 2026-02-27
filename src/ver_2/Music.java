package ver_2;

import java.io.BufferedInputStream;
import java.io.InputStream;
import javazoom.jl.player.Player;

public class Music implements Runnable {

    private final String path;
    private final boolean loop;
    private Player player;
    private volatile boolean playing = true;
    private Thread thread;

    public Music(String path, boolean loop) {
        this.path = path;
        this.loop = loop;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            do {
                InputStream is =
                    getClass().getResourceAsStream("../Asset/" + path);
                if (is == null) {
                    System.out.println("Music not found: " + path);
                    return;
                }

                player = new Player(new BufferedInputStream(is));
                player.play();
            } while (loop && playing);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        playing = false;
        if (player != null) {
            player.close();
        }
        if (thread != null) {
            thread.interrupt();
        }
    }
}
