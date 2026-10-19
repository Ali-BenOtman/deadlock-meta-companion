package com.deadlockmeta.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import com.deadlockmeta.backend.ingestion.IngestionService;
import com.deadlockmeta.backend.retention.RetentionResult;
import com.deadlockmeta.backend.retention.RetentionService;

class RetentionIntegrationTest extends IntegrationTestBase {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private RetentionService retentionService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        resetDatabase();
        when(assetsService.isShopItem(anyLong())).thenReturn(true);
    }

    @Test
    void deletesMatchesOlderThanRetentionAndKeepsRecentOnes() throws Exception {
        // 5 old matches (1 Sept) and 5 recent ones (10 Oct): the old ones are over 21
        // days before the newest.
        ingestionService.ingestFile(TestData.writeMatches(tempDir, "old.parquet", 1000, 5, "2026-09-01 12:00:00", 85));
        ingestionService
                .ingestFile(TestData.writeMatches(tempDir, "recent.parquet", 2000, 5, "2026-10-10 12:00:00", 85));

        RetentionResult result = retentionService.deleteOldMatches();

        assertEquals(5, result.matchesDeleted());
        assertEquals(60, result.playersDeleted());
        assertEquals(360, result.itemPurchasesDeleted());

        assertEquals(5, count("matches"));
        assertEquals(60, count("match_players"));
        assertEquals(360, count("item_purchases"));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM matches WHERE match_id < 2000", Long.class));
    }

    @Test
    void keepsEverythingWhenNothingIsOld() throws Exception {
        ingestionService
                .ingestFile(TestData.writeMatches(tempDir, "recent.parquet", 2000, 5, "2026-10-10 12:00:00", 85));

        RetentionResult result = retentionService.deleteOldMatches();

        assertEquals(0, result.matchesDeleted());
        assertEquals(5, count("matches"));
    }
}