package com.deadlockmeta.backend.stats;

public record ItemStat(
        long itemId,
        String itemName,
        int buyers,
        double buyRate,
        double winRate,
        double winRateDelta,
        int medianBuySeconds) {
}