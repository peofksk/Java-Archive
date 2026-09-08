package state.gameplay;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class GameResult {

    private final int score;
    private final int maxCombo;
    private final double accuracy;
    private final EnumMap<Judgement, Integer> judgementCounts;

    public GameResult(int score, int maxCombo, double accuracy, Map<Judgement, Integer> judgementCounts) {
        this.score = score;
        this.maxCombo = maxCombo;
        this.accuracy = accuracy;
        this.judgementCounts = new EnumMap<>(Judgement.class);

        for (Judgement judgement : Judgement.values()) {
            if (judgement == Judgement.NONE) {
                continue;
            }
            this.judgementCounts.put(judgement, judgementCounts.getOrDefault(judgement, 0));
        }
    }

    public int getScore() {
        return score;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public int getJudgementCount(Judgement judgement) {
        if (judgement == null || judgement == Judgement.NONE) {
            return 0;
        }
        return judgementCounts.getOrDefault(judgement, 0);
    }

    public Map<Judgement, Integer> getJudgementCounts() {
        return Collections.unmodifiableMap(judgementCounts);
    }

    public int getTotalJudgementCount() {
        int total = 0;
        for (int count : judgementCounts.values()) {
            total += count;
        }
        return total;
    }
}