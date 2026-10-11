package com.deadlockmeta.backend.ingestion;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
public class IngestionDevController {

    private final IngestionService ingestionService;
    private final DirectoryIngestionService directoryIngestionService;
    private final String sampleFile;
    private final String dataDir;

    public IngestionDevController(IngestionService ingestionService,
                                  DirectoryIngestionService directoryIngestionService,
                                  @Value("${app.data.sample-file}") String sampleFile,
                                  @Value("${app.data.dir}") String dataDir) {
        this.ingestionService = ingestionService;
        this.directoryIngestionService = directoryIngestionService;
        this.sampleFile = sampleFile;
        this.dataDir = dataDir;
    }

    @PostMapping("/ingest-sample")
    public IngestionResult ingestSample() throws SQLException {
        return ingestionService.ingestFile(sampleFile);
    }

    @PostMapping("/ingest-all")
    public DirectoryIngestionResult ingestAll() throws IOException, SQLException {
        return directoryIngestionService.ingestDirectory(Path.of(dataDir));
    }
}