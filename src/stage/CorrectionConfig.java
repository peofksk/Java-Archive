package stage;

public class CorrectionConfig {

    private final String levelName;
    private final String musicPath;
    private final String backgroundImageKey;
    private final String noteFilePath;
    private final Object noteColor;

    public CorrectionConfig(String levelName, String musicPath,
                            String backgroundImageKey, String noteFilePath,
                            Object noteColor) {
        this.levelName = levelName;
        this.musicPath = musicPath;
        this.backgroundImageKey = backgroundImageKey;
        this.noteFilePath = noteFilePath;
        this.noteColor = noteColor;
    }

    public String getLevelName() {
        return levelName;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public String getBackgroundImageKey() {
        return backgroundImageKey;
    }

    public String getNoteFilePath() {
        return noteFilePath;
    }

    public Object getNoteColor() {
        return noteColor;
    }
}