package com.deadlockmeta.backend.assets;

import java.util.Map;

public record AssetsSummary(
        int heroes,
        int items,
        int ranks,
        Map<String, Long> itemsByType,
        String heroId1,
        String badge13,
        String itemExample) {
}