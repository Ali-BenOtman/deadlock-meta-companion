package com.deadlockmeta.backend.feedback;

/**
 * The body of POST /api/feedback. "website" is a hidden field that only bots
 * fill in.
 */
public record FeedbackRequest(Integer rating, String message, String website) {
}