package com.deadlockmeta.backend.ingestion;

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IngestionDevController {

    private final IngestionService ingestionService;
    private final String sampleFile;

    public IngestionDevController(IngestionService ingestionService,
            @Value("${app.data.sample-file}") String sampleFile) {
        this.ingestionService = ingestionService;
        this.sampleFile = sampleFile;
    }

    @PostMapping("/api/dev/ingest-sample")
    public IngestionResult ingestSample() throws SQLException {
        return ingestionService.ingestFile(sampleFile);
    }
}