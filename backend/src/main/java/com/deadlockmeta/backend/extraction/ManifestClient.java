package com.deadlockmeta.backend.extraction;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ManifestClient {

    public static final String BASE_URL = "https://data.deadlock-api.com";

    private final RestClient restClient = RestClient.create(BASE_URL);

    public List<Manifest.ManifestFile> matchPlayerFiles() {
        Manifest manifest = restClient.get()
                .uri("/v1/manifest.json")
                .retrieve()
                .body(Manifest.class);
        if (manifest == null || manifest.tables() == null || !manifest.tables().containsKey("match_player")) {
            throw new IllegalStateException("Manifest has no match_player table");
        }
        return manifest.tables().get("match_player").files();
    }
}