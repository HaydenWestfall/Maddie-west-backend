package com.maddiewest.events.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maddiewest.events.document.RentalItemMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RentalItemResponse {

    @JsonProperty("_id")
    private String id;

    private String name;
    private String description;
    private List<String> images;
    private String category;
    private int totalQuantity;
    private BigDecimal price;
    private RentalItemMetadata metadata;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Defaults to totalQuantity when no date range was supplied with the request.
     */
    private int availableQuantity;
}
