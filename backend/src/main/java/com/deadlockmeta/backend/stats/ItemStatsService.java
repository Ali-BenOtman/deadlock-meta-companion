package com.deadlockmeta.backend.stats;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.deadlockmeta.backend.assets.AssetsService;

@Service
public class ItemStatsService {

    private static final int MIN_BUYERS = 100;

    private static final String ITEM_STATS_SQL = """
            WITH hero AS (
                SELECT SUM(games) AS players,
                       SUM(wins)::numeric / NULLIF(SUM(games), 0) AS win_rate
                FROM hero_tier_stats
                WHERE hero_id = ? AND tier >= ?
            )
            SELECT item_id,
                   SUM(buyers) AS buyers,
                   ROUND(100.0 * SUM(buyers) / (SELECT players FROM hero), 2) AS buy_rate,
                   ROUND(100.0 * SUM(wins) / SUM(buyers), 2) AS win_rate,
                   ROUND(100.0 * (SUM(wins)::numeric / SUM(buyers) - (SELECT win_rate FROM hero)), 2) AS win_rate_delta,
                   ROUND(SUM(median_buy_s::numeric * buyers) / SUM(buyers))::int AS median_buy_s
            FROM item_tier_stats
            WHERE hero_id = ? AND tier >= ?
            GROUP BY item_id
            HAVING SUM(buyers) >= ?
            ORDER BY win_rate_delta DESC
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AssetsService assetsService;

    public ItemStatsService(JdbcTemplate jdbcTemplate, AssetsService assetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetsService = assetsService;
    }

    @Cacheable("itemStats")
    public List<ItemStat> itemStats(int heroId, int minTier) {
        return jdbcTemplate.query(
                ITEM_STATS_SQL,
                (rows, rowNumber) -> new ItemStat(
                        rows.getLong("item_id"),
                        assetsService.itemName(rows.getLong("item_id")),
                        rows.getInt("buyers"),
                        rows.getDouble("buy_rate"),
                        rows.getDouble("win_rate"),
                        rows.getDouble("win_rate_delta"),
                        rows.getInt("median_buy_s"),
                        assetsService.itemImageUrl(rows.getLong("item_id"))),
                heroId,
                minTier,
                heroId,
                minTier,
                MIN_BUYERS);
    }
}