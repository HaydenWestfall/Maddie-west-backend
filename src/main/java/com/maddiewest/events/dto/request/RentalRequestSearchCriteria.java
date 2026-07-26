package com.maddiewest.events.dto.request;

import com.maddiewest.events.document.RentalRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Filtering and sorting options for browsing rental requests from the admin UI.
 * All fields are optional; unset fields are ignored when building the query.
 */
@Getter
@Builder
public class RentalRequestSearchCriteria {

    /** Free-text term matched against customer name, email, status and item names. */
    private final String search;

    /** Substring match against the requester's name. */
    private final String customer;

    /** Substring match against the requester's email. */
    private final String email;

    /** Exact status filter. */
    private final RentalRequestStatus status;

    /** Inclusive lower bound (by day) for the event start date. */
    private final LocalDate dateFrom;

    /** Inclusive upper bound (by day) for the event start date. */
    private final LocalDate dateTo;

    /** Sort key from the UI: customer, email, eventDate, items, total, status or submitted. */
    private final String sort;

    /** Sort direction: {@code asc} or {@code desc} (defaults to descending). */
    private final String direction;
}
