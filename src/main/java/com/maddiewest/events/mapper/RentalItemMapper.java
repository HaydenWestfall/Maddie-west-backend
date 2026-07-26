package com.maddiewest.events.mapper;

import com.maddiewest.events.document.RentalItem;
import com.maddiewest.events.dto.request.RentalItemCreateRequest;
import com.maddiewest.events.dto.request.RentalItemUpdateRequest;
import com.maddiewest.events.dto.response.RentalItemResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class RentalItemMapper {

    public RentalItemResponse toResponse(RentalItem item, int availableQuantity) {
        return RentalItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .images(item.getImages())
                .category(item.getCategory())
                .totalQuantity(item.getTotalQuantity())
                .price(item.getPrice())
                .metadata(item.getMetadata())
                .active(item.isActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .availableQuantity(availableQuantity)
                .build();
    }

    public RentalItemResponse toResponse(RentalItem item) {
        return toResponse(item, item.getTotalQuantity());
    }

    public RentalItem toDocument(RentalItemCreateRequest request) {
        RentalItem item = new RentalItem();
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setImages(request.getImages() != null ? request.getImages() : new ArrayList<>());
        item.setCategory(request.getCategory());
        item.setTotalQuantity(request.getTotalQuantity());
        item.setPrice(request.getPrice());
        item.setMetadata(request.getMetadata());
        item.setActive(true);
        return item;
    }

    public void updateDocument(RentalItem item, RentalItemUpdateRequest request) {
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setImages(request.getImages() != null ? request.getImages() : new ArrayList<>());
        item.setCategory(request.getCategory());
        item.setTotalQuantity(request.getTotalQuantity());
        item.setPrice(request.getPrice());
        item.setMetadata(request.getMetadata());
        item.setActive(request.isActive());
    }
}
