package com.deadlockmeta.backend.retention;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

    private static final int BATCH_SIZE = 2_000;

    private static final String CUTOFF_SQL = "SELECT MAX(start_time) - make_interval(days => ?) FROM matches";

    private static final String NEXT_BATCH_SQL = """
            SELECT match_id
            FROM matches
            WHERE start_time < ?
            ORDER BY match_id
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final int retentionDays;

    public RetentionService(JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            @Value("${app.retention.days}") int retentionDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.retentionDays = retentionDays;
    }

    public RetentionResult deleteOldMatches() {
        long startedAt = System.nanoTime();

        OffsetDateTime cutoff = jdbcTemplate.queryForObject(CUTOFF_SQL, OffsetDateTime.class, retentionDays);
        if (cutoff == null) {
            return new RetentionResult(0, 0, 0, 0);
        }

        long matches = 0;
        long players = 0;
        long items = 0;

        while (true) {
            List<Long> ids = jdbcTemplate.queryForList(NEXT_BATCH_SQL, Long.class, cutoff, BATCH_SIZE);
            if (ids.isEmpty()) {
                break;
            }
            Long[] batch = ids.toArray(Long[]::new);

            // One small transaction per batch, children before parents (foreign keys).
            long[] deleted = transactionTemplate.execute(status -> new long[] {
                    deleteByMatchIds("DELETE FROM item_purchases WHERE match_id = ANY(?)", batch),
                    deleteByMatchIds("DELETE FROM match_players WHERE match_id = ANY(?)", batch),
                    deleteByMatchIds("DELETE FROM matches WHERE match_id = ANY(?)", batch) });

            items += deleted[0];
            players += deleted[1];
            matches += deleted[2];

            if (deleted[2] == 0) {
                log.warn("Retention batch deleted no matches; stopping to avoid an endless loop");
                break;
            }
        }

        double seconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;
        log.info("Retention (older than {}): deleted {} matches, {} players, {} item purchases in {} s",
                cutoff, matches, players, items, String.format("%.1f", seconds));
        return new RetentionResult(matches, players, items, seconds);
    }

    private long deleteByMatchIds(String sql, Long[] matchIds) {
        return jdbcTemplate.update(sql,
                statement -> statement.setArray(1, statement.getConnection().createArrayOf("bigint", matchIds)));
    }
}