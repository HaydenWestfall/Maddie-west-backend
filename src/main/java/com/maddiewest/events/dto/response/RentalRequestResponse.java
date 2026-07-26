package com.maddiewest.events.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maddiewest.events.document.AgreementAcknowledgment;
import com.maddiewest.events.document.RentalDateRange;
import com.maddiewest.events.document.RentalRequestStatus;
import com.maddiewest.events.document.RequesterInfo;
import com.maddiewest.events.document.StatusHistoryEntry;
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
    private AgreementAcknowledgment agreement;
    private RentalRequestStatus status;
    private List<StatusHistoryEntry> statusHistory;
    private BigDecimal totalPrice;
    private Instant createdAt;
    private Instant updatedAt;
}
