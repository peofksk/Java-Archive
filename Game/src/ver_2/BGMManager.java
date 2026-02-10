package ver_2;

public class BGMManager {

    private Music current;

    public void play(String path, boolean loop) {
        stop();
        current = new Music(path, loop);
    }

    public void stop() {
        if (current != null) {
            current.stop();
            current = null;
        }
    }
}
