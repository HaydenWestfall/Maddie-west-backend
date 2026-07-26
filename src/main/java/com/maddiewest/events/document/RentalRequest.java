package com.maddiewest.rentalservice.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "rental_requests")
@CompoundIndex(name = "status_daterange_idx",
        def = "{'status': 1, 'dateRange.startDate': 1, 'dateRange.endDate': 1}")
public class RentalRequest {

    @Id
    @JsonProperty("_id")
    private String id;

    private List<RentalRequestLineItem> items = new ArrayList<>();

    private RentalDateRange dateRange;

    private RequesterInfo requester;

    private AgreementAcknowledgment agreement;

    private RentalRequestStatus status = RentalRequestStatus.PENDING;

    private List<StatusHistoryEntry> statusHistory = new ArrayList<>();

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
