package com.maddiewest.events.exception;

public class AdminAccessDeniedException extends RuntimeException {

    public AdminAccessDeniedException(String message) {
        super(message);
    }
}
