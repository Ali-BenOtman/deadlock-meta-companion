package com.deadlockmeta.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.junit.jupiter.api.BeforeEach;

class SecurityIntegrationTest extends IntegrationTestBase {

    private static final String ALLOWED_ORIGIN = "https://frontend.test";

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

    @Test
    void allowedOriginGetsCorsHeader() {
        ResponseEntity<String> response = api().get().uri("/api/meta")
                .header("Origin", ALLOWED_ORIGIN)
                .retrieve()
                .toEntity(String.class);
        assertEquals(ALLOWED_ORIGIN, response.getHeaders().getAccessControlAllowOrigin());
    }

    @Test
    void otherOriginIsRejected() {
        assertThrows(HttpClientErrorException.Forbidden.class,
                () -> api().get().uri("/api/meta")
                        .header("Origin", "https://evil.example")
                        .retrieve()
                        .toBodilessEntity());
    }

    @Test
    void statusWorksOnEmptyDatabase() {
        Map<String, Object> status = api().get().uri("/api/status").retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        assertEquals(0, ((Number) status.get("matchesUsed")).intValue());
        assertNull(status.get("refreshedAt"));
        assertNull(status.get("windowStart"));
        assertNull(status.get("windowEnd"));
    }

    @Test
    void invalidMinTierOnStatusReturns400() {
        assertThrows(HttpClientErrorException.BadRequest.class,
                () -> api().get().uri("/api/status?minTier=50").retrieve().toBodilessEntity());
    }
}