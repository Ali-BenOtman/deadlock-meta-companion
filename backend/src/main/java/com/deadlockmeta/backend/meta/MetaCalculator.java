package com.deadlockmeta.backend.meta;

public final class MetaCalculator {

    public static final double PRIOR_WIN_RATE = 50.0;
    public static final int SMOOTHING_GAMES = 200;
    public static final int MIN_GAMES = 500;

    public static final int TREND_MIN_GAMES = 500;
    public static final double TREND_THRESHOLD = 1.5;

    private MetaCalculator() {
    }

    public static double smoothedWinRate(int wins, int games) {
        double priorWins = SMOOTHING_GAMES * PRIOR_WIN_RATE / 100.0;
        double smoothed = 100.0 * (wins + priorWins) / (games + SMOOTHING_GAMES);
        return Math.round(smoothed * 100.0) / 100.0;
    }

    public static Tier tierFor(double smoothedWinRate, int games) {
        if (games < MIN_GAMES) {
            return Tier.NOT_ENOUGH_DATA;
        }
        double delta = smoothedWinRate - PRIOR_WIN_RATE;
        if (delta >= 3) {
            return Tier.S;
        }
        if (delta >= 1) {
            return Tier.A;
        }
        if (delta > -1) {
            return Tier.B;
        }
        if (delta > -3) {
            return Tier.C;
        }
        return Tier.D;
    }

    public static double winRateChange(int recentGames, int recentWins, int previousGames, int previousWins) {
        double change = 100.0 * recentWins / recentGames - 100.0 * previousWins / previousGames;
        return Math.round(change * 100.0) / 100.0;
    }

    public static Trend trendFor(int recentGames, int recentWins, int previousGames, int previousWins) {
        if (recentGames < TREND_MIN_GAMES || previousGames < TREND_MIN_GAMES) {
            return Trend.NOT_ENOUGH_DATA;
        }
        double change = winRateChange(recentGames, recentWins, previousGames, previousWins);
        if (change > TREND_THRESHOLD) {
            return Trend.RISING;
        }
        if (change < -TREND_THRESHOLD) {
            return Trend.FALLING;
        }
        return Trend.STEADY;
    }
}