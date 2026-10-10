package com.deadlockmeta.backend.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RankAsset(int tier, String name) {
}