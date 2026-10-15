package com.deadlockmeta.backend.stats;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.deadlockmeta.backend.assets.AssetsService;

@Service
public class HeroStatsService {

    private static final String HERO_STATS_SQL = """
            WITH totals AS (
                SELECT SUM(matches) AS matches
                FROM tier_match_totals
                WHERE tier >= ?
            )
            SELECT hero_id,
                   SUM(games) AS games,
                   SUM(wins) AS wins,
                   ROUND(100.0 * SUM(wins) / SUM(games), 2) AS win_rate,
                   ROUND(100.0 * SUM(games) / (SELECT matches FROM totals), 2) AS pick_rate
            FROM hero_tier_stats
            WHERE tier >= ?
            GROUP BY hero_id
            ORDER BY win_rate DESC
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AssetsService assetsService;

    public HeroStatsService(JdbcTemplate jdbcTemplate, AssetsService assetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetsService = assetsService;
    }

    @Cacheable("heroStats")
    public List<HeroStat> heroStats(int minTier) {
        return jdbcTemplate.query(
                HERO_STATS_SQL,
                (rows, rowNumber) -> new HeroStat(
                        rows.getInt("hero_id"),
                        assetsService.heroName(rows.getInt("hero_id")),
                        rows.getInt("games"),
                        rows.getInt("wins"),
                        rows.getDouble("win_rate"),
                        rows.getDouble("pick_rate")),
                minTier,
                minTier);
    }
}