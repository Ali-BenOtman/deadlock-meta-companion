package com.deadlockmeta.backend.stats;

import java.time.Instant;

/**
 * Describes the data behind the current statistics: when they were last
 * refreshed, which period they cover and how many matches they are based on.
 * The three timestamps are null before the first refresh has run.
 */
public record DataStatus(
        Instant refreshedAt,
        Instant windowStart,
        Instant windowEnd,
        int matchesUsed) {
}