package com.deadlockmeta.backend.stats;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.deadlockmeta.backend.assets.AssetsService;

@Service
public class ItemPhaseStatsService {

    private static final int MIN_BUYERS = 100;
    private static final String[] PHASE_NAMES = { "EARLY", "MID", "LATE" };

    private static final String ITEM_PHASE_SQL = """
            WITH hero AS (
                SELECT SUM(games) AS players,
                       SUM(wins)::numeric / NULLIF(SUM(games), 0) AS win_rate
                FROM hero_tier_stats
                WHERE hero_id = ? AND tier >= ?
            )
            SELECT phase,
                   item_id,
                   SUM(buyers) AS buyers,
                   ROUND(100.0 * SUM(buyers) / (SELECT players FROM hero), 2) AS buy_rate,
                   ROUND(100.0 * SUM(wins) / SUM(buyers), 2) AS win_rate,
                   ROUND(100.0 * (SUM(wins)::numeric / SUM(buyers) - (SELECT win_rate FROM hero)), 2) AS win_rate_delta
            FROM item_phase_stats
            WHERE hero_id = ? AND tier >= ?
            GROUP BY phase, item_id
            HAVING SUM(buyers) >= ?
            ORDER BY phase, win_rate_delta DESC
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AssetsService assetsService;

    public ItemPhaseStatsService(JdbcTemplate jdbcTemplate, AssetsService assetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetsService = assetsService;
    }

    /**
     * Not cached: the table is small and indexed by hero, so reading it directly
     * means the answer is never older than the last committed refresh.
     */
    public List<PhaseItemStat> itemPhaseStats(int heroId, int minTier) {
        return jdbcTemplate.query(
                ITEM_PHASE_SQL,
                (rows, rowNumber) -> new PhaseItemStat(
                        PHASE_NAMES[rows.getInt("phase") - 1],
                        rows.getLong("item_id"),
                        assetsService.itemName(rows.getLong("item_id")),
                        rows.getInt("buyers"),
                        rows.getDouble("buy_rate"),
                        rows.getDouble("win_rate"),
                        rows.getDouble("win_rate_delta"),
                        assetsService.itemImageUrl(rows.getLong("item_id"))),
                heroId,
                minTier,
                heroId,
                minTier,
                MIN_BUYERS);
    }
}