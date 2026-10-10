package com.deadlockmeta.backend.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HeroAsset(
        int id,
        String name,
        @JsonProperty("player_selectable") Boolean playerSelectable,
        Boolean disabled) {
}