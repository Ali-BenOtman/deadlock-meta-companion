package com.deadlockmeta.backend.ingestion;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@RestController
@RequestMapping("/api/dev")
public class IngestionDevController {

    private final DirectoryIngestionService directoryIngestionService;
    private final String dataDir;

    public IngestionDevController(DirectoryIngestionService directoryIngestionService,
            @Value("${app.data.dir}") String dataDir) {
        this.directoryIngestionService = directoryIngestionService;
        this.dataDir = dataDir;
    }

    @PostMapping("/ingest-all")
    public DirectoryIngestionResult ingestAll() throws IOException, SQLException {
        return directoryIngestionService.ingestDirectory(Path.of(dataDir));
    }
}