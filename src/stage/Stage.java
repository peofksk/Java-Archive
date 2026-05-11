package stage;

public class Stage {

	private final String levelName;
	private final String titleImageKey;
	private final String samplePath;
	private final String musicPath;
	private final String backgroundImageKey;
	private final String noteFileKey;
	private final double musicBPM;
	private final double musicOffsetSeconds;

	public Stage(String levelName, String titleImageKey, String sampleMusicPath, String musicPath,
	             String backgroundImageKey, String noteFileKey, double musicBPM,
	             double musicOffsetSeconds) {
		this.levelName = levelName;
		this.titleImageKey = titleImageKey;
		this.samplePath = sampleMusicPath;
		this.musicPath = musicPath;
		this.backgroundImageKey = backgroundImageKey;
		this.noteFileKey = noteFileKey;
		this.musicBPM = musicBPM;
		this.musicOffsetSeconds = musicOffsetSeconds;
	}

	public String getLevelName() {
		return levelName;
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

	public String getNoteFileKey() {
		return noteFileKey;
	}

	public double getMusicBPM() {
		return musicBPM;
	}

	public double getMusicOffsetSeconds() {
		return musicOffsetSeconds;
	}
}