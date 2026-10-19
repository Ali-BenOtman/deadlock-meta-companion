package com.deadlockmeta.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import com.deadlockmeta.backend.ingestion.IngestionResult;
import com.deadlockmeta.backend.ingestion.IngestionService;

class IngestionIntegrationTest extends IntegrationTestBase {

    @Autowired
    private IngestionService ingestionService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        resetDatabase();
        when(assetsService.isShopItem(anyLong())).thenReturn(true);
    }

    @Test
    void ingestingTheSameFileTwiceInsertsRowsOnlyOnce() throws Exception {
        String file = TestData.writeMatches(tempDir, "matches.parquet", 1000, 3, "2026-10-10 12:00:00", 85);

        IngestionResult first = ingestionService.ingestFile(file);
        assertEquals(3, first.matchesInserted());
        assertEquals(36, first.playersInserted());
        assertEquals(216, first.itemPurchasesInserted());

        IngestionResult second = ingestionService.ingestFile(file);
        assertEquals(0, second.matchesInserted());
        assertEquals(0, second.playersInserted());
        assertEquals(0, second.itemPurchasesInserted());

        assertEquals(3, count("matches"));
        assertEquals(36, count("match_players"));
        assertEquals(216, count("item_purchases"));
    }
}