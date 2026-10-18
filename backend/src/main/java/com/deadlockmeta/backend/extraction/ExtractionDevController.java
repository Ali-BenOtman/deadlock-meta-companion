package com.deadlockmeta.backend.extraction;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@RestController
@RequestMapping("/api/dev")
public class ExtractionDevController {

    private static final int MAX_PARTITIONS = 10;

    private final ExtractionService extractionService;
    private final String dataDir;

    public ExtractionDevController(ExtractionService extractionService,
            @Value("${app.data.dir}") String dataDir) {
        this.extractionService = extractionService;
        this.dataDir = dataDir;
    }

    @PostMapping("/extract")
    public ExtractionResult extract(@RequestParam int fromPartition, @RequestParam int toPartition)
            throws SQLException, IOException {
        if (toPartition < fromPartition || toPartition - fromPartition + 1 > MAX_PARTITIONS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Partition range must be ascending and at most " + MAX_PARTITIONS + " partitions");
        }
        String fileName = "extract_p" + fromPartition + "_p" + toPartition + ".parquet";
        return extractionService.extractPartitions(fromPartition, toPartition, Path.of(dataDir, fileName));
    }
}