package com.deadlockmeta.backend.ingestion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deadlockmeta.backend.assets.AssetsService;

@Service
public class IngestionService {

    private static final String PLAYERS_VIEW = """
            CREATE TEMP VIEW ranked_players AS
            SELECT *
            FROM read_parquet('%s')
            WHERE match_mode = 'Ranked'
              AND game_mode = 'Normal'
              AND account_id <> 0
            QUALIFY ROW_NUMBER() OVER (
                PARTITION BY match_id, account_id
                ORDER BY created_at DESC
            ) = 1
            """;

    private static final String PLAYERS_QUERY = """
            SELECT match_id,
                   CAST(epoch(start_time) AS BIGINT) AS start_epoch,
                   duration_s, match_mode, average_badge, winning_team,
                   account_id, hero_id, team, won
            FROM ranked_players
            """;

    private static final String ITEMS_QUERY = """
            SELECT match_id,
                   account_id,
                   UNNEST("items.item_id")     AS item_id,
                   UNNEST("items.game_time_s") AS bought_at_s,
                   UNNEST("items.sold_time_s") AS sold_at_s
            FROM ranked_players
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AssetsService assetsService;

    public IngestionService(JdbcTemplate jdbcTemplate, AssetsService assetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetsService = assetsService;
    }

    @Transactional
    public IngestionResult ingestFile(String parquetPath) throws SQLException {
        Map<Long, Object[]> matches = new LinkedHashMap<>();
        List<Object[]> players = new ArrayList<>();
        List<Object[]> items = new ArrayList<>();

        try (Connection duck = DriverManager.getConnection("jdbc:duckdb:");
                Statement statement = duck.createStatement()) {

            statement.execute(PLAYERS_VIEW.formatted(parquetPath));

            try (ResultSet rows = statement.executeQuery(PLAYERS_QUERY)) {
                while (rows.next()) {
                    long matchId = rows.getLong("match_id");
                    matches.putIfAbsent(matchId, new Object[] {
                            matchId,
                            OffsetDateTime.ofInstant(Instant.ofEpochSecond(rows.getLong("start_epoch")),
                                    ZoneOffset.UTC),
                            rows.getInt("duration_s"),
                            rows.getString("match_mode"),
                            nullableInt(rows, "average_badge"),
                            rows.getString("winning_team") });
                    players.add(new Object[] {
                            matchId,
                            rows.getLong("account_id"),
                            rows.getInt("hero_id"),
                            rows.getString("team"),
                            rows.getBoolean("won") });
                }
            }

            try (ResultSet rows = statement.executeQuery(ITEMS_QUERY)) {
                while (rows.next()) {
                    long itemId = rows.getLong("item_id");
                    if (!assetsService.isShopItem(itemId)) {
                        continue;
                    }
                    int soldAt = rows.getInt("sold_at_s");
                    items.add(new Object[] {
                            rows.getLong("match_id"),
                            rows.getLong("account_id"),
                            itemId,
                            rows.getInt("bought_at_s"),
                            soldAt == 0 ? null : soldAt });
                }
            }
        }

        int matchesInserted = countInserted(jdbcTemplate.batchUpdate("""
                INSERT INTO matches (match_id, start_time, duration_s, match_mode, average_badge, winning_team)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (match_id) DO NOTHING
                """, new ArrayList<>(matches.values())));

        int playersInserted = countInserted(jdbcTemplate.batchUpdate("""
                INSERT INTO match_players (match_id, account_id, hero_id, team, won)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (match_id, account_id) DO NOTHING
                """, players));

        int itemsInserted = countInserted(jdbcTemplate.batchUpdate("""
                INSERT INTO item_purchases (match_id, account_id, item_id, bought_at_s, sold_at_s)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """, items));

        return new IngestionResult(matchesInserted, playersInserted, itemsInserted);
    }

    private static Integer nullableInt(ResultSet rows, String column) throws SQLException {
        int value = rows.getInt(column);
        return rows.wasNull() ? null : value;
    }

    private static int countInserted(int[] results) {
        return Arrays.stream(results).filter(count -> count > 0).sum();
    }
}