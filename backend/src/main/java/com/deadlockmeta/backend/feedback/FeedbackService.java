package com.deadlockmeta.backend.feedback;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {

    public static final int MAX_MESSAGE_LENGTH = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redis;
    private final int perVisitorPerHour;
    private final int perDay;
    // Random at every start, so a stored hash cannot be matched back to an address
    // later.
    private final byte[] salt = new byte[16];

    public FeedbackService(JdbcTemplate jdbcTemplate, StringRedisTemplate redis,
            @Value("${app.feedback.per-visitor-per-hour:5}") int perVisitorPerHour,
            @Value("${app.feedback.per-day:200}") int perDay) {
        this.jdbcTemplate = jdbcTemplate;
        this.redis = redis;
        this.perVisitorPerHour = perVisitorPerHour;
        this.perDay = perDay;
        new SecureRandom().nextBytes(salt);
    }

    public static String dailyKey() {
        return "feedback:day:" + LocalDate.now(ZoneOffset.UTC);
    }

    /**
     * Counts one attempt for this visitor and says whether it is still within the
     * hourly limit.
     */
    public boolean visitorAllowed(String address) {
        return count("feedback:visitor:" + hash(address), Duration.ofHours(1)) <= perVisitorPerHour;
    }

    /**
     * Counts one attempt for the whole site today and says whether it is still
     * within the daily cap.
     */
    public boolean dayAllowed() {
        return count(dailyKey(), Duration.ofDays(2)) <= perDay;
    }

    public void save(Integer rating, String message) {
        jdbcTemplate.update(
                "INSERT INTO feedback (rating, message) VALUES (?, ?)",
                new Object[] { rating, message },
                new int[] { Types.SMALLINT, Types.VARCHAR });
    }

    // SET NX EX creates the key together with its expiry, so a crash between the
    // two steps cannot
    // leave a counter that never expires; INCR keeps that expiry.
    private long count(String key, Duration ttl) {
        redis.opsForValue().setIfAbsent(key, "0", ttl);
        Long value = redis.opsForValue().increment(key);
        return value == null ? Long.MAX_VALUE : value;
    }

    private String hash(String address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return HexFormat.of().formatHex(digest.digest(address.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}