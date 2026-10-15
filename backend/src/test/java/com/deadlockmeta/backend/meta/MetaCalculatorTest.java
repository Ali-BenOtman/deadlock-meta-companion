package com.deadlockmeta.backend.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MetaCalculatorTest {

    @Test
    void smallSampleIsPulledStronglyTowardAverage() {
        assertEquals(51.43, MetaCalculator.smoothedWinRate(8, 10), 0.01);
    }

    @Test
    void largeSampleBarelyMoves() {
        assertEquals(55.40, MetaCalculator.smoothedWinRate(3182, 5724), 0.01);
    }

    @Test
    void tiersFollowDistanceFromAverage() {
        assertEquals(Tier.S, MetaCalculator.tierFor(55.40, 5724));
        assertEquals(Tier.A, MetaCalculator.tierFor(51.50, 1000));
        assertEquals(Tier.B, MetaCalculator.tierFor(50.00, 1000));
        assertEquals(Tier.C, MetaCalculator.tierFor(49.00, 1000));
        assertEquals(Tier.D, MetaCalculator.tierFor(46.00, 1000));
    }

    @Test
    void heroesBelowMinimumGamesHaveNoTier() {
        assertEquals(Tier.NOT_ENOUGH_DATA, MetaCalculator.tierFor(60.00, 499));
    }

    @Test
    void winRateUpByMoreThanThresholdIsRising() {
        // 55% this week vs 52% last week: +3 points
        assertEquals(Trend.RISING, MetaCalculator.trendFor(1000, 550, 1000, 520));
    }

    @Test
    void winRateDownByMoreThanThresholdIsFalling() {
        // 50% vs 52%: -2 points
        assertEquals(Trend.FALLING, MetaCalculator.trendFor(1000, 500, 1000, 520));
    }

    @Test
    void smallChangeIsSteady() {
        // 53% vs 52%: +1 point
        assertEquals(Trend.STEADY, MetaCalculator.trendFor(1000, 530, 1000, 520));
    }

    @Test
    void changeExactlyAtThresholdIsSteady() {
        // 53.5% vs 52.0%: exactly +1.5, which must be MORE than 1.5 to count as rising
        assertEquals(Trend.STEADY, MetaCalculator.trendFor(1000, 535, 1000, 520));
    }

    @Test
    void tooFewGamesInEitherWeekHasNoTrend() {
        assertEquals(Trend.NOT_ENOUGH_DATA, MetaCalculator.trendFor(400, 220, 1000, 520));
        assertEquals(Trend.NOT_ENOUGH_DATA, MetaCalculator.trendFor(1000, 520, 499, 260));
    }
}