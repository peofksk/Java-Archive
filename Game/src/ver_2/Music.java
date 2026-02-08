package ver_2;

import java.io.BufferedInputStream;
import java.io.InputStream;
import javazoom.jl.player.Player;

public class Music {

    private final String path;
    private Player player;
    private Thread thread;

    public Music(String path) {
        this.path = path;
    }

    public void play() {
        stop();

        thread = new Thread(() -> {
            try {
                InputStream is = getClass().getResourceAsStream("../Asset/" + path);
                if (is == null) return;

                BufferedInputStream bis = new BufferedInputStream(is);
                player = new Player(bis);
                player.play();
            } catch (Exception e) {
                System.out.println("Music play error: " + path);
                e.printStackTrace();
            }
        });

        thread.start();
    }

    public void stop() {
        if (player != null) {
            player.close();
            player = null;
        }
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }
}
