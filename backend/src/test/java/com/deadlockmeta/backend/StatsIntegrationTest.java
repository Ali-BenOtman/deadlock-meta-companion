package com.deadlockmeta.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import com.deadlockmeta.backend.ingestion.IngestionService;
import com.deadlockmeta.backend.stats.StatsRefreshService;

class StatsIntegrationTest extends IntegrationTestBase {

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

        // 100 complete matches at badge 85 (tier 8): heroes 1-6 always win, heroes 7-12
        // always lose.
        String file = TestData.writeMatches(tempDir, "matches.parquet", 1000, 100, "2026-10-10 12:00:00", 85);
        ingestionService.ingestFile(file);
        statsRefreshService.refresh();
    }

    @Test
    void metaComputesWinRatesPickRatesAndSmoothing() {
        List<Map<String, Object>> meta = getList("/api/meta");
        assertEquals(12, meta.size());

        Map<String, Object> winner = findBy(meta, "heroId", 1);
        assertEquals(100, number(winner, "games"), 0.001);
        assertEquals(100.0, number(winner, "winRate"), 0.001);
        assertEquals(100.0, number(winner, "pickRate"), 0.001);
        assertEquals(66.67, number(winner, "smoothedWinRate"), 0.001);
        assertEquals("NOT_ENOUGH_DATA", winner.get("tier"));
        assertEquals("NOT_ENOUGH_DATA", winner.get("trend"));
        assertNull(winner.get("winRateChange"));

        Map<String, Object> loser = findBy(meta, "heroId", 7);
        assertEquals(0.0, number(loser, "winRate"), 0.001);
        assertEquals(33.33, number(loser, "smoothedWinRate"), 0.001);
    }

    @Test
    void rankFilterExcludesLowerTiers() {
        assertTrue(getList("/api/meta?minTier=9").isEmpty());
        assertEquals(12, getList("/api/meta?minTier=8").size());
    }

    @Test
    void itemStatsCountBuyersWinRatesAndMedianBuyTime() {
        List<Map<String, Object>> items = getList("/api/heroes/1/items");
        assertEquals(6, items.size());

        Map<String, Object> item3 = findBy(items, "itemId", 3);
        assertEquals(100, number(item3, "buyers"), 0.001);
        assertEquals(100.0, number(item3, "buyRate"), 0.001);
        assertEquals(100.0, number(item3, "winRate"), 0.001);
        assertEquals(0.0, number(item3, "winRateDelta"), 0.001);
        assertEquals(180, number(item3, "medianBuySeconds"), 0.001);
    }

    @Test
    void buildStatsGroupTheFirstSixItems() {
        List<Map<String, Object>> builds = getList("/api/heroes/1/builds");
        assertEquals(1, builds.size());

        Map<String, Object> build = builds.get(0);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), build.get("itemIds"));
        assertEquals(100, number(build, "games"), 0.001);
        assertEquals(100.0, number(build, "winRate"), 0.001);
    }

    @Test
    void statusDescribesTheLastRefresh() {
        Map<String, Object> status = getMap("/api/status");
        assertEquals(100, number(status, "matchesUsed"), 0.001);

        Instant start = Instant.parse((String) status.get("windowStart"));
        Instant end = Instant.parse((String) status.get("windowEnd"));
        assertEquals(14, Duration.between(start, end).toDays());

        Instant refreshed = Instant.parse((String) status.get("refreshedAt"));
        assertTrue(Duration.between(refreshed, Instant.now()).abs().toMinutes() < 10);
    }

    @Test
    void statusMatchCountFollowsTheRankFilter() {
        assertEquals(100, number(getMap("/api/status?minTier=8"), "matchesUsed"), 0.001);
        assertEquals(0, number(getMap("/api/status?minTier=9"), "matchesUsed"), 0.001);
    }

    @Test
    void dailyStatsGiveOneRowPerDayForAHero() {
        List<Map<String, Object>> days = getList("/api/heroes/1/daily");
        assertEquals(5, days.size());

        long games = 0;
        LocalDate previous = null;
        for (Map<String, Object> day : days) {
            LocalDate date = LocalDate.parse((String) day.get("day"));
            if (previous != null) {
                assertTrue(date.isAfter(previous));
            }
            previous = date;
            games += ((Number) day.get("games")).longValue();
            assertEquals(number(day, "games"), number(day, "wins"), 0.001);
            assertEquals(100.0, number(day, "winRate"), 0.001);
        }
        assertEquals(100, games);
    }

    @Test
    void dailyStatsForALosingHeroAndAFilteredRank() {
        List<Map<String, Object>> losing = getList("/api/heroes/7/daily");
        assertEquals(5, losing.size());
        assertEquals(0.0, number(losing.get(0), "wins"), 0.001);
        assertEquals(0.0, number(losing.get(0), "winRate"), 0.001);

        assertTrue(getList("/api/heroes/1/daily?minTier=9").isEmpty());
    }

    private List<Map<String, Object>> getList(String uri) {
        return api().get().uri(uri).retrieve().body(new ParameterizedTypeReference<>() {
        });
    }

    private Map<String, Object> getMap(String uri) {
        return api().get().uri(uri).retrieve().body(new ParameterizedTypeReference<>() {
        });
    }

    private static Map<String, Object> findBy(List<Map<String, Object>> rows, String key, long value) {
        return rows.stream()
                .filter(row -> ((Number) row.get(key)).longValue() == value)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No row with " + key + " = " + value));
    }

    private static double number(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).doubleValue();
    }
}