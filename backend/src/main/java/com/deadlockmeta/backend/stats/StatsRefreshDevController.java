package com.deadlockmeta.backend.stats;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
public class StatsRefreshDevController {

    private final StatsRefreshService statsRefreshService;

    public StatsRefreshDevController(StatsRefreshService statsRefreshService) {
        this.statsRefreshService = statsRefreshService;
    }

    @PostMapping("/refresh-stats")
    public StatsRefreshResult refreshStats() {
        return statsRefreshService.refresh();
    }
}