package com.deadlockmeta.backend.meta;

import java.util.Comparator;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.deadlockmeta.backend.stats.HeroStatsService;

@Service
public class MetaService {

    private final HeroStatsService heroStatsService;

    public MetaService(HeroStatsService heroStatsService) {
        this.heroStatsService = heroStatsService;
    }

    @Cacheable("meta")
    public List<HeroTier> heroTiers(int minTier) {
        return heroStatsService.heroStats(minTier).stream()
                .map(hero -> {
                    double smoothed = MetaCalculator.smoothedWinRate(hero.wins(), hero.games());
                    return new HeroTier(
                            hero.heroId(),
                            hero.heroName(),
                            MetaCalculator.tierFor(smoothed, hero.games()),
                            smoothed,
                            hero.winRate(),
                            hero.pickRate(),
                            hero.games());
                })
                .sorted(Comparator
                        .comparing((HeroTier hero) -> hero.tier() == Tier.NOT_ENOUGH_DATA)
                        .thenComparing(Comparator.comparingDouble(HeroTier::smoothedWinRate).reversed()))
                .toList();
    }
}