package com.deadlockmeta.backend.extraction;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Manifest(Map<String, Table> tables) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Table(List<ManifestFile> files) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ManifestFile(
            String key,
            String kind,
            Integer partition,
            @JsonProperty("rows_by_partition") Map<String, Long> rowsByPartition,
            long rows,
            long bytes) {

        public boolean touchesPartitions(int fromPartition, int toPartition) {
            if (partition != null) {
                return partition >= fromPartition && partition <= toPartition;
            }
            if (rowsByPartition == null) {
                return false;
            }
            return rowsByPartition.keySet().stream()
                    .mapToInt(Integer::parseInt)
                    .anyMatch(p -> p >= fromPartition && p <= toPartition);
        }
    }
}