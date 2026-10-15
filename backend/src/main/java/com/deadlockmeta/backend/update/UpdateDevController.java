package com.deadlockmeta.backend.update;

import java.io.IOException;
import java.sql.SQLException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
public class UpdateDevController {

    private final DataUpdateService dataUpdateService;

    public UpdateDevController(DataUpdateService dataUpdateService) {
        this.dataUpdateService = dataUpdateService;
    }

    @PostMapping("/run-update")
    public DataUpdateResult runUpdate() throws SQLException, IOException {
        return dataUpdateService.runUpdate();
    }
}