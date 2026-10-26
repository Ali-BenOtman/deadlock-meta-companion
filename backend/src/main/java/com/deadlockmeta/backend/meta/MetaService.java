package com.deadlockmeta.backend.meta;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.deadlockmeta.backend.assets.AssetsService;
import com.deadlockmeta.backend.stats.HeroStatsService;

@Service
public class MetaService {

    private final HeroStatsService heroStatsService;
    private final HeroTrendService heroTrendService;
    private final AssetsService assetsService;

    public MetaService(HeroStatsService heroStatsService, HeroTrendService heroTrendService,
            AssetsService assetsService) {
        this.heroStatsService = heroStatsService;
        this.heroTrendService = heroTrendService;
        this.assetsService = assetsService;
    }

    @Cacheable("meta")
    public List<HeroTier> heroTiers(int minTier) {
        Map<Integer, HeroTrend> trends = heroTrendService.trends(minTier);

        return heroStatsService.heroStats(minTier).stream()
                .map(hero -> {
                    double smoothed = MetaCalculator.smoothedWinRate(hero.wins(), hero.games());

                    Trend trend = Trend.NOT_ENOUGH_DATA;
                    Double change = null;
                    HeroTrend weeks = trends.get(hero.heroId());
                    if (weeks != null) {
                        trend = MetaCalculator.trendFor(weeks.recentGames(), weeks.recentWins(),
                                weeks.previousGames(), weeks.previousWins());
                        if (trend != Trend.NOT_ENOUGH_DATA) {
                            change = MetaCalculator.winRateChange(weeks.recentGames(), weeks.recentWins(),
                                    weeks.previousGames(), weeks.previousWins());
                        }
                    }

                    return new HeroTier(
                            hero.heroId(),
                            hero.heroName(),
                            MetaCalculator.tierFor(smoothed, hero.games()),
                            smoothed,
                            hero.winRate(),
                            hero.pickRate(),
                            hero.games(),
                            trend,
                            change,
                            assetsService.heroImageUrl(hero.heroId()));
                })
                .sorted(Comparator
                        .comparing((HeroTier hero) -> hero.tier() == Tier.NOT_ENOUGH_DATA)
                        .thenComparing(Comparator.comparingDouble(HeroTier::smoothedWinRate).reversed()))
                .toList();
    }
}