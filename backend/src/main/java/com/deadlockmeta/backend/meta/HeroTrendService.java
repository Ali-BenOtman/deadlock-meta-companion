package com.deadlockmeta.backend.meta;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HeroTrendService {

    private static final String TRENDS_SQL = """
            WITH bounds AS (
                SELECT (window_end AT TIME ZONE 'UTC')::date AS end_day
                FROM stats_refresh
                WHERE id = 1
            )
            SELECT h.hero_id,
                   COALESCE(SUM(h.games) FILTER (WHERE h.day > b.end_day - 7), 0) AS recent_games,
                   COALESCE(SUM(h.wins)  FILTER (WHERE h.day > b.end_day - 7), 0) AS recent_wins,
                   COALESCE(SUM(h.games) FILTER (WHERE h.day <= b.end_day - 7 AND h.day > b.end_day - 14), 0) AS previous_games,
                   COALESCE(SUM(h.wins)  FILTER (WHERE h.day <= b.end_day - 7 AND h.day > b.end_day - 14), 0) AS previous_wins
            FROM hero_daily_stats h
            CROSS JOIN bounds b
            WHERE h.tier >= ?
            GROUP BY h.hero_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public HeroTrendService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<Integer, HeroTrend> trends(int minTier) {
        return jdbcTemplate.query(
                TRENDS_SQL,
                (rows, rowNumber) -> new HeroTrend(
                        rows.getInt("hero_id"),
                        rows.getInt("recent_games"),
                        rows.getInt("recent_wins"),
                        rows.getInt("previous_games"),
                        rows.getInt("previous_wins")),
                minTier)
                .stream()
                .collect(Collectors.toMap(HeroTrend::heroId, Function.identity()));
    }
}