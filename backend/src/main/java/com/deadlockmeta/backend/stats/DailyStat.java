package com.deadlockmeta.backend.stats;

import java.time.LocalDate;

/** One day of a hero's results within the statistics window. */
public record DailyStat(LocalDate day, int games, int wins, double winRate) {
}