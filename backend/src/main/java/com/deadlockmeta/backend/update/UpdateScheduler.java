package com.deadlockmeta.backend.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.update.enabled", havingValue = "true")
public class UpdateScheduler {

    private static final Logger log = LoggerFactory.getLogger(UpdateScheduler.class);

    private final DataUpdateService dataUpdateService;

    public UpdateScheduler(DataUpdateService dataUpdateService) {
        this.dataUpdateService = dataUpdateService;
    }

    @Scheduled(fixedDelayString = "${app.update.interval}", initialDelayString = "${app.update.initial-delay}")
    public void scheduledUpdate() {
        log.info("Scheduled data update starting");
        try {
            DataUpdateResult result = dataUpdateService.runUpdate();
            log.info("Scheduled data update finished: {}", result.status());
        } catch (Exception e) {
            log.error("Scheduled data update failed", e);
        }
    }
}