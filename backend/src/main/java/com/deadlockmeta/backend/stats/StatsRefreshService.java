package com.deadlockmeta.backend.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsRefreshService {

    private static final Logger log = LoggerFactory.getLogger(StatsRefreshService.class);

    private static final String CREATE_COMPLETE_MATCHES = """
            CREATE TEMP TABLE complete_matches ON COMMIT DROP AS
            SELECT m.match_id,
                   (m.average_badge / 10)::smallint AS tier,
                   (m.start_time AT TIME ZONE 'UTC')::date AS day
            FROM matches m
            JOIN match_players mp ON mp.match_id = m.match_id
            WHERE m.average_badge IS NOT NULL
              AND m.start_time >= (SELECT MAX(start_time) FROM matches) - make_interval(days => %d)
            GROUP BY m.match_id, m.average_badge, m.start_time
            HAVING COUNT(*) = 12
            """;

    private static final String INSERT_TIER_TOTALS = """
            INSERT INTO tier_match_totals (tier, matches)
            SELECT tier, COUNT(*)
            FROM complete_matches
            GROUP BY tier
            """;

    private static final String INSERT_HERO_STATS = """
            INSERT INTO hero_tier_stats (hero_id, tier, games, wins)
            SELECT mp.hero_id, cm.tier, COUNT(*), SUM(CASE WHEN mp.won THEN 1 ELSE 0 END)
            FROM match_players mp
            JOIN complete_matches cm ON cm.match_id = mp.match_id
            GROUP BY mp.hero_id, cm.tier
            """;

    private static final String INSERT_HERO_DAILY_STATS = """
            INSERT INTO hero_daily_stats (hero_id, tier, day, games, wins)
            SELECT mp.hero_id, cm.tier, cm.day, COUNT(*), SUM(CASE WHEN mp.won THEN 1 ELSE 0 END)
            FROM match_players mp
            JOIN complete_matches cm ON cm.match_id = mp.match_id
            GROUP BY mp.hero_id, cm.tier, cm.day
            """;

    private static final String INSERT_ITEM_STATS = """
            INSERT INTO item_tier_stats (hero_id, item_id, tier, buyers, wins, median_buy_s)
            SELECT mp.hero_id, fb.item_id, cm.tier,
                   COUNT(*),
                   SUM(CASE WHEN mp.won THEN 1 ELSE 0 END),
                   ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY fb.first_buy_s))::int
            FROM (
                SELECT match_id, account_id, item_id, MIN(bought_at_s) AS first_buy_s
                FROM item_purchases
                GROUP BY match_id, account_id, item_id
            ) fb
            JOIN complete_matches cm ON cm.match_id = fb.match_id
            JOIN match_players mp ON mp.match_id = fb.match_id AND mp.account_id = fb.account_id
            GROUP BY mp.hero_id, fb.item_id, cm.tier
            """;

    private static final String INSERT_BUILD_STATS = """
            WITH first_buys AS (
                SELECT ip.match_id, ip.account_id, ip.item_id, MIN(ip.bought_at_s) AS first_buy_s
                FROM item_purchases ip
                JOIN complete_matches cm ON cm.match_id = ip.match_id
                GROUP BY ip.match_id, ip.account_id, ip.item_id
            ),
            ordered_buys AS (
                SELECT match_id, account_id, item_id,
                       ROW_NUMBER() OVER (PARTITION BY match_id, account_id
                                          ORDER BY first_buy_s, item_id) AS buy_order
                FROM first_buys
            ),
            player_builds AS (
                SELECT match_id, account_id, ARRAY_AGG(item_id ORDER BY item_id) AS build
                FROM ordered_buys
                WHERE buy_order <= 6
                GROUP BY match_id, account_id
                HAVING COUNT(*) = 6
            )
            INSERT INTO build_tier_stats (hero_id, tier, build_key, build, games, wins)
            SELECT mp.hero_id, cm.tier, array_to_string(pb.build, '-'), pb.build,
                   COUNT(*), SUM(CASE WHEN mp.won THEN 1 ELSE 0 END)
            FROM player_builds pb
            JOIN complete_matches cm ON cm.match_id = pb.match_id
            JOIN match_players mp ON mp.match_id = pb.match_id AND mp.account_id = pb.account_id
            GROUP BY mp.hero_id, cm.tier, pb.build
            """;

    private static final String UPSERT_REFRESH = """
            INSERT INTO stats_refresh (id, refreshed_at, duration_ms, matches_used, window_start, window_end)
            SELECT 1, now(), ?, ?, MAX(start_time) - make_interval(days => ?), MAX(start_time)
            FROM matches
            ON CONFLICT (id) DO UPDATE
            SET refreshed_at = EXCLUDED.refreshed_at,
                duration_ms  = EXCLUDED.duration_ms,
                matches_used = EXCLUDED.matches_used,
                window_start = EXCLUDED.window_start,
                window_end   = EXCLUDED.window_end
            """;

    private final JdbcTemplate jdbcTemplate;
    private final int windowDays;

    public StatsRefreshService(JdbcTemplate jdbcTemplate,
                               @Value("${app.stats.window-days}") int windowDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.windowDays = windowDays;
    }

    @Transactional
    public StatsRefreshResult refresh() {
        long startedAt = System.nanoTime();

        jdbcTemplate.execute("SET LOCAL work_mem = '256MB'");

        step("Complete matches (last " + windowDays + " days)", CREATE_COMPLETE_MATCHES.formatted(windowDays));
        jdbcTemplate.execute("CREATE INDEX ON complete_matches (match_id)");
        jdbcTemplate.execute("ANALYZE complete_matches");
        Integer matchesUsed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM complete_matches", Integer.class);

        jdbcTemplate.update("DELETE FROM tier_match_totals");
        jdbcTemplate.update("DELETE FROM hero_tier_stats");
        jdbcTemplate.update("DELETE FROM hero_daily_stats");
        jdbcTemplate.update("DELETE FROM item_tier_stats");
        jdbcTemplate.update("DELETE FROM build_tier_stats");

        step("Tier totals", INSERT_TIER_TOTALS);
        int heroRows = step("Hero stats", INSERT_HERO_STATS);
        step("Hero daily stats", INSERT_HERO_DAILY_STATS);
        int itemRows = step("Item stats", INSERT_ITEM_STATS);
        int buildRows = step("Build stats", INSERT_BUILD_STATS);

        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        int matches = matchesUsed == null ? 0 : matchesUsed;
        jdbcTemplate.update(UPSERT_REFRESH, durationMs, matches, windowDays);

        log.info("Stats refresh finished in {} s", String.format("%.1f", durationMs / 1000.0));
        return new StatsRefreshResult(matches, heroRows, itemRows, buildRows, durationMs / 1000.0);
    }

    private int step(String label, String sql) {
        long start = System.nanoTime();
        int rows = jdbcTemplate.update(sql);
        log.info("{}: {} rows in {} s", label, rows, String.format("%.1f", (System.nanoTime() - start) / 1e9));
        return rows;
    }
}