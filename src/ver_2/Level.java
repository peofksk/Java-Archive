package ver_2;

public enum Level {

	unwelcomeSchool("title_unwelcome", "/sample_unwelcomeSchool.wav", "/unwelcomeSchool.wav"),

	afterSchoolDessert("title_after", "/sample_afterSchoolDessert.wav", "/afterSchoolDessert.wav"),

	comingSoon("title_coming", "/sample_comingSoon.wav", "/comingSoon.wav");

	private final String titleImageKey;
	private final String samplePath;
	private final String musicPath;

	Level(String titleImageKey, String samplePath, String musicPath) {
		this.titleImageKey = titleImageKey;
		this.samplePath = samplePath;
		this.musicPath = musicPath;
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

	public Level next() {
		int next = ordinal() + 1;
		if (next >= values().length)
			next = values().length - 1;
		return values()[next];
	}

	public Level prev() {
		int prev = ordinal() - 1;
		if (prev < 0)
			prev = 0;
		return values()[prev];
	}
}