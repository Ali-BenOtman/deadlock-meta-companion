package com.deadlockmeta.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.deadlockmeta.backend.assets.AssetsService;
import com.deadlockmeta.backend.stats.StatsCache;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "POSTGRES_PASSWORD=unused",
        "REDIS_PASSWORD=unused",
        "app.update.enabled=false",
        "app.cors.allowed-origins=https://frontend.test"
})
public abstract class IntegrationTestBase {

    // Singleton containers: started once for all test classes, removed by
    // Testcontainers when the JVM exits.
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.4").withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @MockitoBean
    protected AssetsService assetsService;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private StatsCache statsCache;

    @Value("${local.server.port}")
    private int port;

    protected RestClient api() {
        return RestClient.create("http://localhost:" + port);
    }

    /**
     * Empties every table and the stats cache, so each test starts from a known
     * state.
     */
    protected void resetDatabase() {
        jdbc.execute("TRUNCATE item_purchases, match_players, matches, tier_match_totals, hero_tier_stats, "
                + "hero_daily_stats, item_tier_stats, build_tier_stats, stats_refresh, processed_files");
        statsCache.evictAll();
    }

    protected long count(String table) {
        Long rows = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return rows == null ? 0 : rows;
    }
}