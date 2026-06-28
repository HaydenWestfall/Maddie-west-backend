package com.maddiewest.rentalservice.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "rental_items")
public class RentalItem {

    @Id
    @JsonProperty("_id")
    private String id;

    private String name;

    private String description;

    private List<String> images = new ArrayList<>();

    @Indexed
    private String category;

    private int totalQuantity;

    private BigDecimal price;

    private RentalItemMetadata metadata;

    @Indexed
    private boolean active = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
