package com.deadlockmeta.backend.stats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StatusService {

    private static final String REFRESH_SQL = """
            SELECT refreshed_at, window_start, window_end
            FROM stats_refresh
            WHERE id = 1
            """;

    private static final String MATCHES_SQL = """
            SELECT COALESCE(SUM(matches), 0)
            FROM tier_match_totals
            WHERE tier >= ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public StatusService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Not cached: both tables are tiny, and reading them directly means the answer
     * is never older than the last committed refresh.
     */
    public DataStatus status(int minTier) {
        Long matches = jdbcTemplate.queryForObject(MATCHES_SQL, Long.class, minTier);
        int matchesUsed = matches == null ? 0 : matches.intValue();

        List<DataStatus> refresh = jdbcTemplate.query(
                REFRESH_SQL,
                (rows, rowNumber) -> new DataStatus(
                        instant(rows, "refreshed_at"),
                        instant(rows, "window_start"),
                        instant(rows, "window_end"),
                        matchesUsed));

        // Before the first refresh the table is empty: report no timestamps.
        if (refresh.isEmpty()) {
            return new DataStatus(null, null, null, matchesUsed);
        }
        return refresh.get(0);
    }

    private static Instant instant(ResultSet rows, String column) throws SQLException {
        OffsetDateTime value = rows.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}