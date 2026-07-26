package com.maddiewest.rentalservice.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Snapshot of an item at the time a rental request was submitted, so historical
 * requests remain accurate even if the underlying RentalItem is later renamed,
 * repriced, or retired.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RentalRequestLineItem {

    private String itemId;
    private String itemName;
    private int quantity;
    private BigDecimal pricePerItem;
}
