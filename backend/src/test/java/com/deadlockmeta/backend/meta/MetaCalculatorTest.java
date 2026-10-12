package com.deadlockmeta.backend.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MetaCalculatorTest {

    @Test
    void smallSampleIsPulledStronglyTowardAverage() {
        // 8 wins in 10 games is 80% raw, but it is too little data to trust
        assertEquals(51.43, MetaCalculator.smoothedWinRate(8, 10), 0.01);
    }

    @Test
    void largeSampleBarelyMoves() {
        // Rat King, all ranks: 55.59% raw
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
}