package com.maddiewest.events.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RentalRequestLineItemDto {

    @NotBlank
    private String itemId;

    @Min(1)
    private int quantity;
}
