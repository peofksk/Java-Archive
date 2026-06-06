package stage;

public final class ChartName {
    private ChartName() {
    }

    public static String buildKey(String levelName, Difficulty difficulty, int keyCount) {
        return levelName + "_" + difficulty.name() + "_" + keyCount + "K";
    }

    public static String buildPath(String levelName, Difficulty difficulty, int keyCount) {
        return "/charts/" + levelName + "/" + buildKey(levelName, difficulty, keyCount) + ".txt";
    }
}