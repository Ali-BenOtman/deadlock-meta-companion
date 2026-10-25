package com.deadlockmeta.backend.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemAsset(
                long id,
                @JsonProperty("class_name") String className,
                String name,
                String type,
                String image,
                @JsonProperty("image_webp") String imageWebp) {
}