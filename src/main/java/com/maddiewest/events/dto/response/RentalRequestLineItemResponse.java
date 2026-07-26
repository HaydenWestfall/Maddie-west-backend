package com.maddiewest.events.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class RentalRequestLineItemResponse {

    private String itemId;
    private String itemName;
    private int quantity;
    private BigDecimal pricePerItem;
    private BigDecimal subtotal;
}
