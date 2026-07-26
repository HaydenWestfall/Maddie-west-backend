package com.maddiewest.rentalservice.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory, per-key fixed-window rate limiter for the public contact form,
 * mirroring the contact-api's limit of 10 requests per 15 minutes per IP. Keeps no external
 * dependency; state is per-instance (adequate for a single-node deployment).
 */
@Component
public class ContactRateLimiter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** Returns true if the request is allowed, false if the caller has exceeded the window limit. */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.start >= WINDOW_MS) {
                return new Window(now);
            }
            existing.count++;
            return existing;
        });
        return window.count <= MAX_REQUESTS;
    }

    private static final class Window {
        final long start;
        int count;

        Window(long start) {
            this.start = start;
            this.count = 1;
        }
    }
}
