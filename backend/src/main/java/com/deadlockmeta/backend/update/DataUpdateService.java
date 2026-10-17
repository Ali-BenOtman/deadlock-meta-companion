package com.deadlockmeta.backend.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.deadlockmeta.backend.extraction.ExtractionService;
import com.deadlockmeta.backend.extraction.Manifest;
import com.deadlockmeta.backend.extraction.ManifestClient;
import com.deadlockmeta.backend.ingestion.IngestionResult;
import com.deadlockmeta.backend.ingestion.IngestionService;
import com.deadlockmeta.backend.retention.RetentionService;
import com.deadlockmeta.backend.stats.StatsCache;
import com.deadlockmeta.backend.stats.StatsRefreshService;

@Service
public class DataUpdateService {

    private static final Logger log = LoggerFactory.getLogger(DataUpdateService.class);

    private final ManifestClient manifestClient;
    private final ExtractionService extractionService;
    private final IngestionService ingestionService;
    private final RetentionService retentionService;
    private final StatsRefreshService statsRefreshService;
    private final StatsCache statsCache;
    private final JdbcTemplate jdbcTemplate;
    private final Path dataDir;
    private final int windowDays;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public DataUpdateService(ManifestClient manifestClient,
            ExtractionService extractionService,
            IngestionService ingestionService,
            RetentionService retentionService,
            StatsRefreshService statsRefreshService,
            StatsCache statsCache,
            JdbcTemplate jdbcTemplate,
            @Value("${app.data.dir}") String dataDir,
            @Value("${app.update.window-days}") int windowDays) {
        this.manifestClient = manifestClient;
        this.extractionService = extractionService;
        this.ingestionService = ingestionService;
        this.retentionService = retentionService;
        this.statsRefreshService = statsRefreshService;
        this.statsCache = statsCache;
        this.jdbcTemplate = jdbcTemplate;
        this.dataDir = Path.of(dataDir);
        this.windowDays = windowDays;
    }

    public DataUpdateResult runUpdate() throws SQLException, IOException {
        if (!running.compareAndSet(false, true)) {
            log.warn("Data update already running, skipping this trigger");
            return new DataUpdateResult("ALREADY_RUNNING", 0, 0, 0, 0, false, 0);
        }
        try {
            return doUpdate();
        } finally {
            running.set(false);
        }
    }

    private DataUpdateResult doUpdate() throws SQLException, IOException {
        long startedAt = System.nanoTime();

        Set<String> processed = new HashSet<>(
                jdbcTemplate.queryForList("SELECT file_key FROM processed_files", String.class));
        Integer knownDeltas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_files WHERE kind = 'delta'", Integer.class);

        List<Manifest.ManifestFile> candidates = manifestClient.matchPlayerFiles().stream()
                .filter(file -> "delta".equals(file.kind()) || "residual".equals(file.kind()))
                .toList();

        // Gap check: we have processed deltas before, but none of the deltas still
        // listed are among them.
        boolean overlap = candidates.stream()
                .anyMatch(file -> "delta".equals(file.kind()) && processed.contains(file.key()));
        if (knownDeltas != null && knownDeltas > 0 && !overlap) {
            log.warn("Possible data gap: no previously processed delta is still listed. "
                    + "Run a partition extraction for the missed days.");
        }

        List<Manifest.ManifestFile> newFiles = candidates.stream()
                .filter(file -> !processed.contains(file.key()))
                .toList();

        if (newFiles.isEmpty()) {
            log.info("Data update: no new files");
            return new DataUpdateResult("NO_NEW_FILES", 0, 0, 0, 0, false, secondsSince(startedAt));
        }

        Path extractFile = dataDir.resolve("update_" + System.currentTimeMillis() + ".parquet");
        try {
            extractionService.extractRecent(newFiles, windowDays, extractFile);
            log.info("Ingesting {}", extractFile.getFileName());
            IngestionResult ingested = ingestionService.ingestFile(
                    extractFile.toAbsolutePath().toString().replace('\\', '/'));

            // Record files only after a successful ingest: a crash before this line means
            // the files are processed again next time, which idempotent ingestion makes
            // harmless.
            jdbcTemplate.batchUpdate(
                    "INSERT INTO processed_files (file_key, kind, rows) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                    newFiles.stream().map(file -> new Object[] { file.key(), file.kind(), file.rows() }).toList());

            // New matches move the newest start time forward, so the oldest day may now be
            // outside retention.
            retentionService.deleteOldMatches();

            boolean refreshed = false;
            if (ingested.playersInserted() > 0) {
                statsRefreshService.refresh();
                statsCache.evictAll();
                refreshed = true;
            }

            double seconds = secondsSince(startedAt);
            log.info("Data update: {} new files, {} matches, {} players, {} items, stats refreshed: {}, {} s",
                    newFiles.size(), ingested.matchesInserted(), ingested.playersInserted(),
                    ingested.itemPurchasesInserted(), refreshed, String.format("%.1f", seconds));
            return new DataUpdateResult("UPDATED", newFiles.size(), ingested.matchesInserted(),
                    ingested.playersInserted(), ingested.itemPurchasesInserted(), refreshed, seconds);
        } finally {
            Files.deleteIfExists(extractFile);
        }
    }

    private static double secondsSince(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000_000.0;
    }
}