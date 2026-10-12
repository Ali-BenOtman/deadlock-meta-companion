package com.deadlockmeta.backend.stats;

import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.deadlockmeta.backend.assets.AssetsService;

@Service
public class BuildStatsService {

    private static final int BUILD_SIZE = 6;
    private static final int MIN_GAMES = 30;
    private static final int MAX_BUILDS = 10;

    private static final String BUILD_STATS_SQL = """
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
                SELECT AVG(CASE WHEN won THEN 1 ELSE 0 END) AS win_rate
                FROM hero_players
            ),
            first_buys AS (
                SELECT ip.match_id, ip.account_id, ip.item_id, MIN(ip.bought_at_s) AS first_buy_s
                FROM item_purchases ip
                JOIN hero_players hp ON hp.match_id = ip.match_id AND hp.account_id = ip.account_id
                GROUP BY ip.match_id, ip.account_id, ip.item_id
            ),
            ordered_buys AS (
                SELECT fb.*,
                       ROW_NUMBER() OVER (PARTITION BY fb.match_id, fb.account_id
                                          ORDER BY fb.first_buy_s, fb.item_id) AS buy_order
                FROM first_buys fb
            ),
            player_builds AS (
                SELECT match_id, account_id, ARRAY_AGG(item_id ORDER BY item_id) AS build
                FROM ordered_buys
                WHERE buy_order <= ?
                GROUP BY match_id, account_id
                HAVING COUNT(*) = ?
            )
            SELECT pb.build,
                   COUNT(*) AS games,
                   ROUND(100.0 * AVG(CASE WHEN hp.won THEN 1 ELSE 0 END), 2) AS win_rate,
                   ROUND(100.0 * (AVG(CASE WHEN hp.won THEN 1 ELSE 0 END)
                                  - (SELECT win_rate FROM hero_baseline)), 2) AS win_rate_delta
            FROM player_builds pb
            JOIN hero_players hp ON hp.match_id = pb.match_id AND hp.account_id = pb.account_id
            GROUP BY pb.build
            HAVING COUNT(*) >= ?
            ORDER BY games DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AssetsService assetsService;

    public BuildStatsService(JdbcTemplate jdbcTemplate, AssetsService assetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetsService = assetsService;
    }

    public List<BuildStat> buildStats(int heroId, int minTier) {
        return jdbcTemplate.query(
                BUILD_STATS_SQL,
                (rows, rowNumber) -> {
                    List<Long> itemIds = Arrays.asList((Long[]) rows.getArray("build").getArray());
                    List<String> itemNames = itemIds.stream().map(assetsService::itemName).toList();
                    return new BuildStat(
                            itemIds,
                            itemNames,
                            rows.getInt("games"),
                            rows.getDouble("win_rate"),
                            rows.getDouble("win_rate_delta"));
                },
                minTier * 10,
                heroId,
                BUILD_SIZE,
                BUILD_SIZE,
                MIN_GAMES,
                MAX_BUILDS);
    }
}