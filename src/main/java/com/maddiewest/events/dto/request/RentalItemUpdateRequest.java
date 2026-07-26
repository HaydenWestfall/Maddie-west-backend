package com.maddiewest.events.dto.request;

import com.maddiewest.events.document.RentalItemMetadata;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class RentalItemUpdateRequest {

    @NotBlank
    private String name;

    private String description;

    private List<String> images;

    private String category;

    @PositiveOrZero
    private int totalQuantity;

    @NotNull
    @PositiveOrZero
    private BigDecimal price;

    private RentalItemMetadata metadata;

    private boolean active = true;
}
