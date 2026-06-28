package com.maddiewest.rentalservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maddiewest.rentalservice.document.RentalDateRange;
import com.maddiewest.rentalservice.document.RentalRequestStatus;
import com.maddiewest.rentalservice.document.RequesterInfo;
import com.maddiewest.rentalservice.document.StatusHistoryEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RentalRequestResponse {

    @JsonProperty("_id")
    private String id;

    private List<RentalRequestLineItemResponse> items;
    private RentalDateRange dateRange;
    private RequesterInfo requester;
    private RentalRequestStatus status;
    private List<StatusHistoryEntry> statusHistory;
    private BigDecimal totalPrice;
    private Instant createdAt;
    private Instant updatedAt;
}
