package com.deadlockmeta.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.junit.jupiter.api.BeforeEach;

class SecurityIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        resetDatabase();
    }

    @Test
    void devEndpointsDoNotExistWithoutDevProfile() {
        assertThrows(HttpClientErrorException.NotFound.class,
                () -> api().post().uri("/api/dev/run-update").retrieve().toBodilessEntity());
    }

    @Test
    void unknownHeroReturns404() {
        assertThrows(HttpClientErrorException.NotFound.class,
                () -> api().get().uri("/api/heroes/999999/items").retrieve().toBodilessEntity());
        assertThrows(HttpClientErrorException.NotFound.class,
                () -> api().get().uri("/api/heroes/999999/builds").retrieve().toBodilessEntity());
    }

    @Test
    void invalidMinTierReturns400() {
        assertThrows(HttpClientErrorException.BadRequest.class,
                () -> api().get().uri("/api/meta?minTier=50").retrieve().toBodilessEntity());
    }

    @Test
    void metaWorksOnEmptyDatabase() {
        String body = api().get().uri("/api/meta").retrieve().body(String.class);
        assertEquals("[]", body);
    }
}