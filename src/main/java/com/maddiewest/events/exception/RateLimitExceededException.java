package com.maddiewest.events.exception;

/**
 * Thrown when a client exceeds the allowed request rate for a public endpoint.
 * Mapped to HTTP 429 by {@link GlobalExceptionHandler}.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
