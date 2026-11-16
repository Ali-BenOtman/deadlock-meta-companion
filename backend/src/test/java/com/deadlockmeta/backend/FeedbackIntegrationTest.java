package com.deadlockmeta.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import com.deadlockmeta.backend.feedback.FeedbackService;

class FeedbackIntegrationTest extends IntegrationTestBase {

    private static final String ALLOWED_ORIGIN = "https://frontend.test";

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        resetDatabase();
    }

    // The visitor is chosen with X-Forwarded-For, as Caddy does in production; null
    // means no header.
    private ResponseEntity<Void> post(Map<String, Object> body, String visitor) {
        var request = api().post().uri("/api/feedback").contentType(MediaType.APPLICATION_JSON);
        if (visitor != null) {
            request = request.header("X-Forwarded-For", visitor);
        }
        return request.body(body).retrieve().toBodilessEntity();
    }

    @Test
    void aMessageWithARatingIsStoredTrimmed() {
        ResponseEntity<Void> response = post(Map.of("rating", 4, "message", "  Nice site  "), null);

        assertEquals(201, response.getStatusCode().value());
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT rating, message FROM feedback");
        assertEquals(1, rows.size());
        assertEquals(4, ((Number) rows.get(0).get("rating")).intValue());
        assertEquals("Nice site", rows.get(0).get("message"));
    }

    @Test
    void theRatingIsOptional() {
        post(Map.of("message", "No stars, just words"), null);

        List<Map<String, Object>> rows = jdbc.queryForList("SELECT rating, message FROM feedback");
        assertEquals(1, rows.size());
        assertNull(rows.get(0).get("rating"));
    }

    @Test
    void anEmptyOrMissingMessageIsRejected() {
        assertThrows(HttpClientErrorException.BadRequest.class, () -> post(Map.of("message", "   "), null));
        assertThrows(HttpClientErrorException.BadRequest.class, () -> post(Map.of("rating", 3), null));
        assertEquals(0, count("feedback"));
    }

    @Test
    void aMessageOver1000CharactersIsRejectedButExactly1000Passes() {
        assertThrows(HttpClientErrorException.BadRequest.class, () -> post(Map.of("message", "x".repeat(1001)), null));
        assertEquals(0, count("feedback"));

        post(Map.of("message", "x".repeat(1000)), null);
        assertEquals(1, count("feedback"));
    }

    @Test
    void aRatingOutsideOneToFiveIsRejected() {
        assertThrows(HttpClientErrorException.BadRequest.class, () -> post(Map.of("rating", 0, "message", "ok"), null));
        assertThrows(HttpClientErrorException.BadRequest.class, () -> post(Map.of("rating", 6, "message", "ok"), null));
        assertEquals(0, count("feedback"));
    }

    @Test
    void aNullCharacterIsRejected() {
        assertThrows(HttpClientErrorException.BadRequest.class,
                () -> post(Map.of("message", "a" + (char) 0 + "b"), null));
        assertEquals(0, count("feedback"));
    }

    @Test
    void theHiddenFieldMakesTheRequestLookSuccessfulButStoresNothing() {
        ResponseEntity<Void> response = post(Map.of("message", "buy now", "website", "http://spam.example"), null);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(0, count("feedback"));
    }

    @Test
    void eachVisitorGetsFiveMessagesAnHour() {
        // The limit is app.feedback.per-visitor-per-hour, 5 by default.
        for (int i = 0; i < 5; i++) {
            post(Map.of("message", "message " + i), "203.0.113.7");
        }
        assertThrows(HttpClientErrorException.TooManyRequests.class,
                () -> post(Map.of("message", "one more"), "203.0.113.7"));

        assertEquals(201, post(Map.of("message", "a different visitor"), "203.0.113.8").getStatusCode().value());
        assertEquals(6, count("feedback"));
    }

    @Test
    void theDailyCapStopsEveryone() {
        // The cap is app.feedback.per-day, 200 by default.
        redis.opsForValue().set(FeedbackService.dailyKey(), "200");

        assertThrows(HttpClientErrorException.TooManyRequests.class,
                () -> post(Map.of("message", "too late today"), "203.0.113.9"));
        assertEquals(0, count("feedback"));
    }

    @Test
    void thePreflightForPostIsAllowedFromTheFrontend() {
        ResponseEntity<Void> response = api().options().uri("/api/feedback")
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .retrieve()
                .toBodilessEntity();

        assertEquals(ALLOWED_ORIGIN, response.getHeaders().getAccessControlAllowOrigin());
        assertTrue(response.getHeaders().getAccessControlAllowMethods().contains(HttpMethod.POST));
    }

    @Test
    void anotherWebsiteCannotPostFromABrowser() {
        assertThrows(HttpClientErrorException.Forbidden.class,
                () -> api().post().uri("/api/feedback")
                        .header("Origin", "https://evil.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("message", "hi"))
                        .retrieve()
                        .toBodilessEntity());
        assertEquals(0, count("feedback"));
    }

    @Test
    void feedbackCannotBeReadThroughTheApi() {
        post(Map.of("message", "private"), null);

        assertThrows(HttpClientErrorException.class,
                () -> api().get().uri("/api/feedback").retrieve().toBodilessEntity());
    }
}