package com.maddiewest.rentalservice.mapper;

import com.maddiewest.rentalservice.document.RentalRequest;
import com.maddiewest.rentalservice.document.RentalRequestLineItem;
import com.maddiewest.rentalservice.dto.response.RentalRequestLineItemResponse;
import com.maddiewest.rentalservice.dto.response.RentalRequestResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RentalRequestMapper {

    public RentalRequestResponse toResponse(RentalRequest request) {
        List<RentalRequestLineItemResponse> items = request.getItems().stream()
                .map(this::toLineItemResponse)
                .toList();

        BigDecimal totalPrice = items.stream()
                .map(RentalRequestLineItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RentalRequestResponse.builder()
                .id(request.getId())
                .items(items)
                .dateRange(request.getDateRange())
                .requester(request.getRequester())
                .agreement(request.getAgreement())
                .status(request.getStatus())
                .statusHistory(request.getStatusHistory())
                .totalPrice(totalPrice)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private RentalRequestLineItemResponse toLineItemResponse(RentalRequestLineItem lineItem) {
        BigDecimal subtotal = lineItem.getPricePerItem().multiply(BigDecimal.valueOf(lineItem.getQuantity()));
        return RentalRequestLineItemResponse.builder()
                .itemId(lineItem.getItemId())
                .itemName(lineItem.getItemName())
                .quantity(lineItem.getQuantity())
                .pricePerItem(lineItem.getPricePerItem())
                .subtotal(subtotal)
                .build();
    }
}
