package com.maddiewest.rentalservice.dto.response;

/**
 * Response body for the public contact form. Matches the shape returned by the standalone
 * contact-api ({@code {"success": ..., "message"|"error": ...}}) so the marketing-site
 * frontend only needs its API URL repointed, not its response handling.
 */
public record ContactFormResponse(boolean success, String message) {

    public static ContactFormResponse ok(String message) {
        return new ContactFormResponse(true, message);
    }
}
