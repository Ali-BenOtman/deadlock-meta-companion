package com.deadlockmeta.backend.stats;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@RestController
@RequestMapping("/api/dev")
public class StatsRefreshDevController {

    private final StatsRefreshService statsRefreshService;
    private final StatsCache statsCache;

    public StatsRefreshDevController(StatsRefreshService statsRefreshService, StatsCache statsCache) {
        this.statsRefreshService = statsRefreshService;
        this.statsCache = statsCache;
    }

    @PostMapping("/refresh-stats")
    public StatsRefreshResult refreshStats() {
        StatsRefreshResult result = statsRefreshService.refresh();
        // refresh() has returned, so its transaction is committed: the new stats are
        // visible.
        statsCache.evictAll();
        return result;
    }
}