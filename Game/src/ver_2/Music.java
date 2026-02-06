package ver_2;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javazoom.jl.player.Player;

public class Music extends Thread {

    private Player player;
    private boolean isLoop;
    private boolean isPlaying = true;
    private String musicPath;

    public Music(String musicPath, boolean isLoop) {
        this.musicPath = musicPath;
        this.isLoop = isLoop;
    }

    @Override
    public void run() {
        try {
            do {
                InputStream is = getClass().getResourceAsStream("/" + musicPath);
                BufferedInputStream bis = new BufferedInputStream(is);
                player = new Player(bis);
                player.play();
            } while (isLoop && isPlaying);
        } catch (Exception e) {
            System.out.println("Music play error: " + musicPath);
            e.printStackTrace();
        }
    }

    public void close() {
        isLoop = false;
        isPlaying = false;
        if (player != null) {
            player.close();
        }
        interrupt();
    }
}
