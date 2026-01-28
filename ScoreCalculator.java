import java.time.Duration;

public class ScoreCalculator {

    private static final int BASE_SCORE = 1200;
    private static final int PEG_BONUS = 120;
    private static final int MOVE_PENALTY = 6;
    private static final int TIME_PENALTY_PER_SECOND = 2;
    private static final int CREDIT_DIVISOR = 200;

    private ScoreCalculator() {
    }

    public static int calculate(Duration duration, int remainingPegs, int moveCount) {
        long seconds = duration == null ? 0 : duration.getSeconds();
        int score = BASE_SCORE;
        score += Math.max(0, (32 - remainingPegs)) * PEG_BONUS;
        score -= moveCount * MOVE_PENALTY;
        score -= seconds * TIME_PENALTY_PER_SECOND;
        return Math.max(0, score);
    }

    public static int calculateCredits(int score) {
        if (score <= 0) {
            return 0;
        }
        return Math.max(1, score / CREDIT_DIVISOR);
    }
}
