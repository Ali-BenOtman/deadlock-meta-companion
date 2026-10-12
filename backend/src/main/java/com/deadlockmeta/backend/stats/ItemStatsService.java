package com.deadlockmeta.backend.stats;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.deadlockmeta.backend.assets.AssetsService;

@Service
public class ItemStatsService {

    private static final int MIN_BUYERS = 100;

    private static final String ITEM_STATS_SQL = """
            WITH complete_matches AS (
                SELECT mp.match_id
                FROM match_players mp
                JOIN matches m ON m.match_id = mp.match_id
                WHERE m.average_badge >= ?
                GROUP BY mp.match_id
                HAVING COUNT(*) = 12
            ),
            hero_players AS (
                SELECT mp.match_id, mp.account_id, mp.won
                FROM match_players mp
                JOIN complete_matches cm ON cm.match_id = mp.match_id
                WHERE mp.hero_id = ?
            ),
            hero_baseline AS (
                SELECT AVG(CASE WHEN won THEN 1 ELSE 0 END) AS win_rate,
                       COUNT(*) AS players
                FROM hero_players
            ),
            item_buys AS (
                SELECT ip.match_id, ip.account_id, ip.item_id, MIN(ip.bought_at_s) AS first_buy_s
                FROM item_purchases ip
                JOIN hero_players hp ON hp.match_id = ip.match_id AND hp.account_id = ip.account_id
                GROUP BY ip.match_id, ip.account_id, ip.item_id
            )
            SELECT ib.item_id,
                   COUNT(*) AS buyers,
                   ROUND(100.0 * COUNT(*) / (SELECT players FROM hero_baseline), 2) AS buy_rate,
                   ROUND(100.0 * AVG(CASE WHEN hp.won THEN 1 ELSE 0 END), 2) AS win_rate,
                   ROUND(100.0 * (AVG(CASE WHEN hp.won THEN 1 ELSE 0 END)
                                  - (SELECT win_rate FROM hero_baseline)), 2) AS win_rate_delta,
                   ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY ib.first_buy_s))::int AS median_buy_s
            FROM item_buys ib
            JOIN hero_players hp ON hp.match_id = ib.match_id AND hp.account_id = ib.account_id
            GROUP BY ib.item_id
            HAVING COUNT(*) >= ?
            ORDER BY win_rate_delta DESC
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AssetsService assetsService;

    public ItemStatsService(JdbcTemplate jdbcTemplate, AssetsService assetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetsService = assetsService;
    }

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
                        rows.getInt("median_buy_s")),
                minTier * 10,
                heroId,
                MIN_BUYERS);
    }
}