package com.deadlockmeta.backend.meta;

public final class MetaCalculator {

    public static final double PRIOR_WIN_RATE = 50.0;
    public static final int SMOOTHING_GAMES = 200;
    public static final int MIN_GAMES = 500;

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
}