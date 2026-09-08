package audio;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class BGMManager {

    private Clip clip;
    private long pausePosition = 0;
    private boolean paused = false;
    private String loadedPath = null;

    public void load(String path) {
        unload();

        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                System.out.println("[BGMManager] Music resource not found: " + path);
                return;
            }

            AudioInputStream ais =
                    AudioSystem.getAudioInputStream(new BufferedInputStream(is));

            clip = AudioSystem.getClip();
            clip.open(ais);

            pausePosition = 0;
            paused = false;
            loadedPath = path;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playLoaded(boolean loop) {
        if (clip == null) {
            return;
        }

        clip.setMicrosecondPosition(0);
        pausePosition = 0;
        paused = false;

        if (loop) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            clip.start();
        }
    }

    public void play(String path, boolean loop) {
        load(path);
        playLoaded(loop);
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.setMicrosecondPosition(0);
        }
        pausePosition = 0;
        paused = false;
    }

    public void unload() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }

        pausePosition = 0;
        paused = false;
        loadedPath = null;
    }

    public void pause() {
        if (clip != null && clip.isRunning()) {
            pausePosition = clip.getMicrosecondPosition();
            clip.stop();
            paused = true;
        }
    }

    public void resume() {
        if (clip != null && paused) {
            clip.setMicrosecondPosition(pausePosition);
            clip.start();
            paused = false;
        }
    }

    public boolean isPlaying() {
        return clip != null && clip.isRunning();
    }

    public boolean isPaused() {
        return clip != null && paused;
    }

    public double getPositionSeconds() {
        if (clip == null) {
            return 0;
        }
        return clip.getMicrosecondPosition() / 1_000_000.0;
    }

    public String getLoadedPath() {
        return loadedPath;
    }
}