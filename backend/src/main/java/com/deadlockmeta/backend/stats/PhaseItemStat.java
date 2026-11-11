package com.deadlockmeta.backend.stats;

/**
 * One item's results in one game phase (EARLY, MID or LATE), by when players
 * first bought it.
 */
public record PhaseItemStat(
        String phase,
        long itemId,
        String itemName,
        int buyers,
        double buyRate,
        double winRate,
        double winRateDelta,
        String imageUrl) {
}