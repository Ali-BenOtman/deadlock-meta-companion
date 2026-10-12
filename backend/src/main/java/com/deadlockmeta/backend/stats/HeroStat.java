package com.deadlockmeta.backend.stats;

public record HeroStat(
        int heroId,
        String heroName,
        int games,
        int wins,
        double winRate,
        double pickRate) {
}