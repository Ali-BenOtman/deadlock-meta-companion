package com.deadlockmeta.backend.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HeroAsset(
                int id,
                String name,
                @JsonProperty("player_selectable") Boolean playerSelectable,
                Boolean disabled,
                HeroImages images) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record HeroImages(
                        @JsonProperty("icon_image_small_webp") String iconSmallWebp,
                        @JsonProperty("icon_hero_card_webp") String iconCardWebp) {
        }
}