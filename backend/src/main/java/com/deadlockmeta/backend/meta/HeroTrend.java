package com.deadlockmeta.backend.meta;

public record HeroTrend(
        int heroId,
        int recentGames,
        int recentWins,
        int previousGames,
        int previousWins) {
}