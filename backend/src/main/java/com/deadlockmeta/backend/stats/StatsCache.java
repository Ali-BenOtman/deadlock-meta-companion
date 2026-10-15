package com.deadlockmeta.backend.stats;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class StatsCache {

    @CacheEvict(cacheNames = { "heroStats", "itemStats", "buildStats", "meta" }, allEntries = true)
    public void evictAll() {
        // The annotation does the work: every entry in these caches is removed.
    }
}