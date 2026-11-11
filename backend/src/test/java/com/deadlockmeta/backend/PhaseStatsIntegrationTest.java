package com.deadlockmeta.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import com.deadlockmeta.backend.ingestion.IngestionService;
import com.deadlockmeta.backend.stats.StatsRefreshService;

class PhaseStatsIntegrationTest extends IntegrationTestBase {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private StatsRefreshService statsRefreshService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
        when(assetsService.isShopItem(anyLong())).thenReturn(true);
        when(assetsService.isKnownHero(anyInt())).thenReturn(true);
        when(assetsService.heroName(anyInt())).thenReturn("Test hero");
        when(assetsService.itemName(anyLong())).thenReturn("Test item");

        // Three batches of 100 matches at badge 85. Heroes 1-6 always win, heroes 7-12
        // always lose.
        // Every player buys items 1-6 in a five-minute window, shifted per batch:
        // early: 1:00 to 6:00. mid: item 1 exactly at 10:00, the rest later. late: item
        // 1 exactly at
        // 20:00, the rest later. So a wrong boundary moves a whole item into the wrong
        // phase.
        ingestionService.ingestFile(
                TestData.writeMatches(tempDir, "early.parquet", 1000, 100, "2026-10-10 12:00:00", 85, 0));
        ingestionService.ingestFile(
                TestData.writeMatches(tempDir, "mid.parquet", 2000, 100, "2026-10-10 12:00:00", 85, 540));
        ingestionService.ingestFile(
                TestData.writeMatches(tempDir, "late.parquet", 3000, 100, "2026-10-10 12:00:00", 85, 1140));
        statsRefreshService.refresh();
    }

    @Test
    void phasesSplitFirstPurchasesAtTenAndTwentyMinutes() {
        List<Map<String, Object>> rows = getList("/api/heroes/1/items/phases");
        assertEquals(18, rows.size());

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String expected = i < 6 ? "EARLY" : i < 12 ? "MID" : "LATE";
            assertEquals(expected, row.get("phase"));
            assertEquals(100, number(row, "buyers"), 0.001);
            assertEquals(33.33, number(row, "buyRate"), 0.001);
            assertEquals(100.0, number(row, "winRate"), 0.001);
            assertEquals(0.0, number(row, "winRateDelta"), 0.001);
        }
    }

    @Test
    void phaseStatsForALosingHeroAndAFilteredRank() {
        List<Map<String, Object>> losing = getList("/api/heroes/7/items/phases");
        assertEquals(18, losing.size());
        for (Map<String, Object> row : losing) {
            assertEquals(0.0, number(row, "winRate"), 0.001);
            assertEquals(0.0, number(row, "winRateDelta"), 0.001);
        }

        assertTrue(getList("/api/heroes/1/items/phases?minTier=9").isEmpty());
    }

    private List<Map<String, Object>> getList(String uri) {
        return api().get().uri(uri).retrieve().body(new ParameterizedTypeReference<>() {
        });
    }

    private static double number(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).doubleValue();
    }
}