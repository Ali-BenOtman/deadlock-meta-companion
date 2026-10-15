package com.deadlockmeta.backend.stats;

import java.io.Serializable;
import java.util.List;

public record BuildStat(
                List<Long> itemIds,
                List<String> itemNames,
                int games,
                double winRate,
                double winRateDelta) implements Serializable {
}