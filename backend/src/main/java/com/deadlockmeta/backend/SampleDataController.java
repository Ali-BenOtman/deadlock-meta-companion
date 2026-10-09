package com.deadlockmeta.backend;

import java.sql.SQLException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleDataController {

    private final SampleDataService sampleDataService;

    public SampleDataController(SampleDataService sampleDataService) {
        this.sampleDataService = sampleDataService;
    }

    @GetMapping("/api/dev/sample-summary")
    public List<MatchModeSummary> sampleSummary() throws SQLException {
        return sampleDataService.summarizeByMatchMode();
    }
}