package ver_2;

public enum Difficulty {
	Easy, Hard, Extreme;

	public Difficulty next() {
		int next = ordinal() + 1;
		if (next >= values().length) {
			next = values().length - 1;
		}
		return values()[next];
	}

	public Difficulty prev() {
		int prev = ordinal() - 1;
		if (prev < 0) {
			prev = 0;
		}
		return values()[prev];
	}
}