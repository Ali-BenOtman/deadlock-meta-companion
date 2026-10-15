package com.deadlockmeta.backend.stats;

public record StatsRefreshResult(
        int matchesUsed,
        int heroRows,
        int itemRows,
        int buildRows,
        double seconds) {
}