package ver_2;

public class Stage {

    private final String titleImageKey;
    private final String samplePath;
    private final String musicPath;
    private final String backgroundImageKey;
    private final String noteFilePath;

    public Stage (String titleImageKey, String sampleMusicPath, String musicPath, String backgroundImageKey, String noteFilePath) {
        this.titleImageKey = titleImageKey;
        this.samplePath = sampleMusicPath;
        this.musicPath = musicPath;
        this.backgroundImageKey = backgroundImageKey;
        this.noteFilePath = noteFilePath;
    }

    public String getTitleImageKey() {
        return titleImageKey;
    }

    public String getSamplePath() {
        return samplePath;
    }

    public String getMusicPath() {
        return musicPath;
    }
    public String getBackgroundImageKey() {
    	return backgroundImageKey;
    }
    public String getNotefilePath() {
    	return noteFilePath;
    }
}