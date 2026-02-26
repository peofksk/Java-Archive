package ver_2;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class BGMManager {

    private Clip clip;

    public void play(String path, boolean loop) {
        stop();

        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                System.out.println("Music resource not found: " + path);
                return;
            }

            AudioInputStream ais =
                    AudioSystem.getAudioInputStream(new BufferedInputStream(is));

            clip = AudioSystem.getClip();
            clip.open(ais);

            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }
}