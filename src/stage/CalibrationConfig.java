package stage;

public class CalibrationConfig {

	private final String levelName;
	private final String musicPath;
	private final String backgroundImageKey;
	private final double musicBPM;
	private final double musicOffset;

	public CalibrationConfig(String levelName, String musicPath, String backgroundImageKey,
	                         double musicBPM, double musicOffset) {
		this.levelName = levelName;
		this.musicPath = musicPath;
		this.backgroundImageKey = backgroundImageKey;
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

	public double getMusicBPM() {
		return musicBPM;
	}

	public double getMusicOffset() {
		return musicOffset;
	}

}