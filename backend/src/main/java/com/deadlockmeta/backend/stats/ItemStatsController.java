package com.deadlockmeta.backend.stats;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/heroes")
public class ItemStatsController {

    private final ItemStatsService itemStatsService;

    public ItemStatsController(ItemStatsService itemStatsService) {
        this.itemStatsService = itemStatsService;
    }

    @GetMapping("/{heroId}/items")
    public List<ItemStat> itemStats(@PathVariable int heroId,
            @RequestParam(defaultValue = "0") int minTier) {
        if (minTier < 0 || minTier > 11) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minTier must be between 0 and 11");
        }
        return itemStatsService.itemStats(heroId, minTier);
    }
}