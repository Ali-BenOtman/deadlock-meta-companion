package com.deadlockmeta.backend;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Builds small Parquet files shaped like data lake extracts, with known
 * outcomes.
 * Each match has 12 players: heroes 1-6 on Team0 (always win), heroes 7-12 on
 * Team1 (always lose).
 * Every player buys items 1-6 at 60, 120, ... 360 seconds. Matches start one
 * hour apart.
 */
public final class TestData {

    private static final String MATCHES_SQL = """
            COPY (
                SELECT m.match_id,
                       TIMESTAMP '%s' + to_hours(CAST(m.n AS BIGINT)) AS start_time,
                       1800 AS duration_s,
                       'Ranked' AS match_mode,
                       'Normal' AS game_mode,
                       %d AS average_badge,
                       'Team0' AS winning_team,
                       m.match_id * 100 + p.slot AS account_id,
                       p.slot + 1 AS hero_id,
                       CASE WHEN p.slot < 6 THEN 'Team0' ELSE 'Team1' END AS team,
                       p.slot < 6 AS won,
                       TIMESTAMP '%s' AS created_at,
                       [1, 2, 3, 4, 5, 6] AS "items.item_id",
                       [60, 120, 180, 240, 300, 360] AS "items.game_time_s",
                       [0, 0, 0, 0, 0, 0] AS "items.sold_time_s"
                FROM (SELECT range AS n, %d + range AS match_id FROM range(%d)) m
                CROSS JOIN (SELECT range AS slot FROM range(12)) p
            ) TO '%s' (FORMAT parquet)
            """;

    private TestData() {
    }

    public static String writeMatches(Path dir, String fileName, long firstMatchId, int matches,
            String firstStart, int averageBadge) throws SQLException {
        String path = dir.resolve(fileName).toAbsolutePath().toString().replace('\\', '/');
        try (Connection duck = DriverManager.getConnection("jdbc:duckdb:");
                Statement statement = duck.createStatement()) {
            statement.execute(MATCHES_SQL.formatted(firstStart, averageBadge, firstStart, firstMatchId, matches, path));
        }
        return path;
    }
}