package com.deadlockmeta.backend.stats;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.deadlockmeta.backend.assets.AssetsService;

@Service
public class BuildStatsService {

    private static final int MIN_GAMES = 30;
    private static final int MAX_BUILDS = 10;

    private static final String BUILD_STATS_SQL = """
            WITH hero AS (
                SELECT SUM(wins)::numeric / NULLIF(SUM(games), 0) AS win_rate
                FROM hero_tier_stats
                WHERE hero_id = ? AND tier >= ?
            )
            SELECT build,
                   SUM(games) AS games,
                   ROUND(100.0 * SUM(wins) / SUM(games), 2) AS win_rate,
                   ROUND(100.0 * (SUM(wins)::numeric / SUM(games) - (SELECT win_rate FROM hero)), 2) AS win_rate_delta
            FROM build_tier_stats
            WHERE hero_id = ? AND tier >= ?
            GROUP BY build
            HAVING SUM(games) >= ?
            ORDER BY games DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AssetsService assetsService;

    public BuildStatsService(JdbcTemplate jdbcTemplate, AssetsService assetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetsService = assetsService;
    }

    @Cacheable("buildStats")
    public List<BuildStat> buildStats(int heroId, int minTier) {
        return jdbcTemplate.query(
                BUILD_STATS_SQL,
                (rows, rowNumber) -> {
                    List<Long> itemIds = Arrays.asList((Long[]) rows.getArray("build").getArray());
                    List<String> itemNames = itemIds.stream().map(assetsService::itemName).toList();
                    List<String> imageUrls = itemIds.stream()
                            .map(assetsService::itemImageUrl)
                            .collect(Collectors.toList());
                    return new BuildStat(
                            itemIds,
                            itemNames,
                            rows.getInt("games"),
                            rows.getDouble("win_rate"),
                            rows.getDouble("win_rate_delta"),
                            imageUrls);
                },
                heroId,
                minTier,
                heroId,
                minTier,
                MIN_GAMES,
                MAX_BUILDS);
    }
}