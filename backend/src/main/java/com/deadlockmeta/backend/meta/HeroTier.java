package com.deadlockmeta.backend.meta;

public record HeroTier(
        int heroId,
        String heroName,
        Tier tier,
        double smoothedWinRate,
        double winRate,
        double pickRate,
        int games) {
}