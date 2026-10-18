package com.deadlockmeta.backend.retention;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@RestController
@RequestMapping("/api/dev")
public class RetentionDevController {

    private final RetentionService retentionService;

    public RetentionDevController(RetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @PostMapping("/retention")
    public RetentionResult runRetention() {
        return retentionService.deleteOldMatches();
    }
}