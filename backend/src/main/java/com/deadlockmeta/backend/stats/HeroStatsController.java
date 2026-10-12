package com.deadlockmeta.backend.stats;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/heroes")
public class HeroStatsController {

    private final HeroStatsService heroStatsService;

    public HeroStatsController(HeroStatsService heroStatsService) {
        this.heroStatsService = heroStatsService;
    }

    @GetMapping("/stats")
    public List<HeroStat> heroStats(@RequestParam(defaultValue = "0") int minTier) {
        if (minTier < 0 || minTier > 11) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minTier must be between 0 and 11");
        }
        return heroStatsService.heroStats(minTier);
    }
}