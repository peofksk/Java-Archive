package ver_2;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javazoom.jl.player.Player;

public class Music extends Thread {

    private final String path;
    private final boolean loop;
    private Player player;
    private volatile boolean isPlaying = true;

    public Music(String path, boolean loop) {
        this.path = path;
        this.loop = loop;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            do {
                InputStream is = getClass().getResourceAsStream("../Asset/" + path);
                if (is == null) {
                    throw new RuntimeException("Music not found: " + path);
                }

                BufferedInputStream bis = new BufferedInputStream(is);
                player = new Player(bis);
                player.play();

                if (!loop) break;

            } while (isPlaying);

        } catch (Exception e) {
            System.out.println("Music play error: " + path);
            e.printStackTrace();
        }
    }

    public void close() {
        isPlaying = false;
        if (player != null) {
            player.close();
        }
    }
}
