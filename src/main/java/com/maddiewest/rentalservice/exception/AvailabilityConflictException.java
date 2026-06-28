package com.maddiewest.rentalservice.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class AvailabilityConflictException extends RuntimeException {

    private final List<ConflictDetail> conflicts;

    public AvailabilityConflictException(String message, List<ConflictDetail> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }

    public record ConflictDetail(String itemId, String itemName, int requestedQuantity, int availableQuantity) {
    }
}
