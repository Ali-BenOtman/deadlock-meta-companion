package com.deadlockmeta.backend.stats;

import java.io.Serializable;

public record ItemStat(
        long itemId,
        String itemName,
        int buyers,
        double buyRate,
        double winRate,
        double winRateDelta,
        int medianBuySeconds,
        String imageUrl) implements Serializable {
}