package com.deadlockmeta.backend.feedback;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void submit(@RequestBody FeedbackRequest body, HttpServletRequest request) {
        // A real person never sees the hidden field, so a filled one means a bot:
        // answer as if it worked, and store nothing.
        if (body.website() != null && !body.website().isBlank()) {
            return;
        }

        String message = body.message() == null ? "" : body.message().trim();
        if (message.isEmpty() || message.length() > FeedbackService.MAX_MESSAGE_LENGTH
                || message.indexOf((char) 0) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must be 1 to 1000 characters");
        }
        Integer rating = body.rating();
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        try {
            if (!feedbackService.visitorAllowed(clientAddress(request)) || !feedbackService.dayAllowed()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many messages");
            }
        } catch (DataAccessException e) {
            // Without Redis the limits cannot be enforced, so stop accepting messages.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Feedback is unavailable");
        }

        feedbackService.save(rating, message);
    }

    // The backend is only reachable through Caddy, which adds the visitor's address
    // to
    // X-Forwarded-For. The last entry is the one Caddy added, so it is used even if
    // a visitor
    // sent a forged header of their own.
    private static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}