package com.deadlockmeta.backend.meta;

import java.io.Serializable;

public record HeroTier(
                int heroId,
                String heroName,
                Tier tier,
                double smoothedWinRate,
                double winRate,
                double pickRate,
                int games) implements Serializable {
}