package com.deadlockmeta.backend.stats;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DailyStatsService {

    private static final String DAILY_SQL = """
            SELECT day,
                   SUM(games) AS games,
                   SUM(wins) AS wins,
                   ROUND(100.0 * SUM(wins) / NULLIF(SUM(games), 0), 2) AS win_rate
            FROM hero_daily_stats
            WHERE hero_id = ? AND tier >= ?
            GROUP BY day
            ORDER BY day
            """;

    private final JdbcTemplate jdbcTemplate;

    public DailyStatsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Not cached: hero_daily_stats is tiny, and reading it directly means the
     * answer is never older than the last committed refresh.
     */
    public List<DailyStat> dailyStats(int heroId, int minTier) {
        return jdbcTemplate.query(
                DAILY_SQL,
                (rows, rowNumber) -> new DailyStat(
                        rows.getObject("day", LocalDate.class),
                        rows.getInt("games"),
                        rows.getInt("wins"),
                        rows.getDouble("win_rate")),
                heroId,
                minTier);
    }
}