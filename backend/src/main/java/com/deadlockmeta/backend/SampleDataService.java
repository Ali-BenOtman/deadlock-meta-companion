package com.deadlockmeta.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SampleDataService {

    private final String sampleFile;

    public SampleDataService(@Value("${app.data.sample-file}") String sampleFile) {
        this.sampleFile = sampleFile;
    }

    public List<MatchModeSummary> summarizeByMatchMode() throws SQLException {
        String sql = """
                SELECT match_mode,
                       COUNT(*) AS players,
                       COUNT(average_badge) AS with_badge
                FROM read_parquet('%s')
                GROUP BY match_mode
                ORDER BY players DESC
                """.formatted(sampleFile);

        List<MatchModeSummary> results = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:");
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(sql)) {

            while (rows.next()) {
                results.add(new MatchModeSummary(
                        rows.getString("match_mode"),
                        rows.getLong("players"),
                        rows.getLong("with_badge")));
            }
        }

        return results;
    }
}