package com.deadlockmeta.backend.ingestion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DirectoryIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DirectoryIngestionService.class);

    private final IngestionService ingestionService;

    public DirectoryIngestionService(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public DirectoryIngestionResult ingestDirectory(Path directory) throws IOException, SQLException {
        List<Path> files;
        try (Stream<Path> paths = Files.list(directory)) {
            files = paths
                    .filter(path -> path.getFileName().toString().endsWith(".parquet"))
                    .sorted()
                    .toList();
        }

        int matches = 0;
        int players = 0;
        int items = 0;

        for (Path file : files) {
            log.info("Ingesting {}", file.getFileName());
            String parquetPath = file.toAbsolutePath().toString().replace('\\', '/');
            IngestionResult result = ingestionService.ingestFile(parquetPath);
            log.info("{}: {} matches, {} players, {} item purchases inserted",
                    file.getFileName(), result.matchesInserted(), result.playersInserted(),
                    result.itemPurchasesInserted());

            matches += result.matchesInserted();
            players += result.playersInserted();
            items += result.itemPurchasesInserted();
        }

        return new DirectoryIngestionResult(files.size(), matches, players, items);
    }
}