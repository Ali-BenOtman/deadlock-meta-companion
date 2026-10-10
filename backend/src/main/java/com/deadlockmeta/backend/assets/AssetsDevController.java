package com.deadlockmeta.backend.assets;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssetsDevController {

    private final AssetsService assetsService;

    public AssetsDevController(AssetsService assetsService) {
        this.assetsService = assetsService;
    }

    @GetMapping("/api/dev/assets-summary")
    public AssetsSummary summary() {
        return new AssetsSummary(
                assetsService.heroCount(),
                assetsService.itemCount(),
                assetsService.rankCount(),
                assetsService.itemCountsByType(),
                assetsService.heroName(1),
                assetsService.rankName(13),
                assetsService.itemName(1548066885L));
    }
}