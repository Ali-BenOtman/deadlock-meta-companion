package com.deadlockmeta.backend.stats;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.deadlockmeta.backend.assets.AssetsService;

@Service
public class HeroStatsService {

    private static final String HERO_STATS_SQL = """
            WITH complete_matches AS (
                SELECT mp.match_id
                FROM match_players mp
                JOIN matches m ON m.match_id = mp.match_id
                WHERE m.average_badge >= ?
                GROUP BY mp.match_id
                HAVING COUNT(*) = 12
            )
            SELECT mp.hero_id,
                   COUNT(*) AS games,
                   SUM(CASE WHEN mp.won THEN 1 ELSE 0 END) AS wins,
                   ROUND(100.0 * AVG(CASE WHEN mp.won THEN 1 ELSE 0 END), 2) AS win_rate,
                   ROUND(100.0 * COUNT(DISTINCT mp.match_id) / (SELECT COUNT(*) FROM complete_matches), 2) AS pick_rate
            FROM match_players mp
            JOIN complete_matches cm ON cm.match_id = mp.match_id
            GROUP BY mp.hero_id
            ORDER BY win_rate DESC
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AssetsService assetsService;

    public HeroStatsService(JdbcTemplate jdbcTemplate, AssetsService assetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetsService = assetsService;
    }

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
                minTier * 10);
    }
}