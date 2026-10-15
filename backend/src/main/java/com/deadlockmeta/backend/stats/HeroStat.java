package com.deadlockmeta.backend.stats;

import java.io.Serializable;

public record HeroStat(
                int heroId,
                String heroName,
                int games,
                int wins,
                double winRate,
                double pickRate) implements Serializable {
}