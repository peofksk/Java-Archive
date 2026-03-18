package stage;

public class CorrectionConfig {

	private final String levelName;
	private final String musicPath;
	private final String backgroundImageKey;
	private final String noteFilePath;
	private final double musicBPM;
	private final double musicOffset;

	public CorrectionConfig(String levelName, String musicPath, String backgroundImageKey, String noteFilePath,
			double musicBPM, double musicOffset) {
		this.levelName = levelName;
		this.musicPath = musicPath;
		this.backgroundImageKey = backgroundImageKey;
		this.noteFilePath = noteFilePath;
		this.musicBPM = musicBPM;
		this.musicOffset = musicOffset;
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

	public double getMusicBPM() {
		return musicBPM;
	}

	public double getMusicOffset() {
		return musicOffset;
	}

}