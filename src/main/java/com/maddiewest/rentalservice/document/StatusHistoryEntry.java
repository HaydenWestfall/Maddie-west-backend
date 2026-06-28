package com.maddiewest.rentalservice.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistoryEntry {

    private RentalRequestStatus status;
    private Instant timestamp;

    /**
     * "system" for the initial PENDING entry created on submission, otherwise the
     * coordinator's username.
     */
    private String changedBy;

    private String reason;
}
