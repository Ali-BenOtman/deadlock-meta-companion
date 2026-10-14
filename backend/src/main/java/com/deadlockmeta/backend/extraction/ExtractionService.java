package com.deadlockmeta.backend.extraction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private static final long MATCH_IDS_PER_PARTITION = 1_000_000L;

    private static final String EXTRACT_SQL = """
            COPY (
                SELECT match_id, start_time, duration_s, match_mode, game_mode,
                       average_badge, winning_team, account_id, hero_id, team, won, created_at,
                       "items.item_id", "items.game_time_s", "items.sold_time_s"
                FROM read_parquet([%s], union_by_name = true)
                WHERE match_id BETWEEN %d AND %d
                  AND match_mode = 'Ranked'
                  AND game_mode = 'Normal'
                  AND match_outcome = 'TeamWin'
            ) TO '%s' (FORMAT parquet)
            """;

    private final ManifestClient manifestClient;

    public ExtractionService(ManifestClient manifestClient) {
        this.manifestClient = manifestClient;
    }

    public ExtractionResult extractPartitions(int fromPartition, int toPartition, Path outputFile)
            throws SQLException, IOException {
        long startedAt = System.nanoTime();

        List<Manifest.ManifestFile> files = manifestClient.matchPlayerFiles().stream()
                .filter(file -> file.touchesPartitions(fromPartition, toPartition))
                .toList();
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No files found for partitions " + fromPartition + "-" + toPartition);
        }

        long sourceBytes = files.stream().mapToLong(Manifest.ManifestFile::bytes).sum();
        String urls = files.stream()
                .map(file -> "'" + ManifestClient.BASE_URL + "/" + file.key() + "'")
                .collect(Collectors.joining(", "));

        long fromMatchId = fromPartition * MATCH_IDS_PER_PARTITION;
        long toMatchId = (toPartition + 1) * MATCH_IDS_PER_PARTITION - 1;
        String outputPath = outputFile.toAbsolutePath().toString().replace('\\', '/');

        log.info("Extracting partitions {}-{} from {} files ({} MB in source)",
                fromPartition, toPartition, files.size(), sourceBytes / 1_000_000);

        long rows;
        try (Connection duck = DriverManager.getConnection("jdbc:duckdb:");
                Statement statement = duck.createStatement()) {

            statement.execute("INSTALL httpfs");
            statement.execute("LOAD httpfs");
            statement.execute(EXTRACT_SQL.formatted(urls, fromMatchId, toMatchId, outputPath));

            try (ResultSet result = statement.executeQuery(
                    "SELECT COUNT(*) FROM read_parquet('" + outputPath + "')")) {
                result.next();
                rows = result.getLong(1);
            }
        }

        double seconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;
        long bytes = Files.size(outputFile);
        log.info("Extracted {} rows ({} MB) in {} s", rows, bytes / 1_000_000, String.format("%.1f", seconds));
        return new ExtractionResult(outputFile.getFileName().toString(), files.size(), sourceBytes, rows, bytes,
                seconds);
    }
}