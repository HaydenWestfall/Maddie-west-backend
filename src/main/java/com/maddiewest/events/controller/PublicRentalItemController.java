package com.maddiewest.events.controller;

import com.maddiewest.events.dto.response.ApiResponse;
import com.maddiewest.events.dto.response.PaginationMeta;
import com.maddiewest.events.dto.response.RentalItemResponse;
import com.maddiewest.events.service.RentalItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Tag(name = "Public catalog", description = "Browse available rental items and inspect availability")
public class PublicRentalItemController {

    private final RentalItemService rentalItemService;

    @GetMapping
    @Operation(summary = "List active rental items", description = "Returns the active catalog with optional filtering and pagination.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Items retrieved successfully")
    })
    public ApiResponse<List<RentalItemResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Page<RentalItemResponse> result = rentalItemService.browseActive(
                category, name, PageRequest.of(Math.max(page - 1, 0), limit));
        return ApiResponse.ok(result.getContent(), PaginationMeta.of(page, limit, result.getTotalElements()));
    }

    @GetMapping("/available")
    @Operation(summary = "Find available items for a date range", description = "Returns items that are available between the supplied start and end dates.")
    public ApiResponse<List<RentalItemResponse>> listAvailable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        Page<RentalItemResponse> result = rentalItemService.browseAvailable(
                category, name, startDate, endDate, PageRequest.of(Math.max(page - 1, 0), limit));
        return ApiResponse.ok(result.getContent(), PaginationMeta.of(page, limit, result.getTotalElements()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a rental item by id", description = "Returns item details and optional availability for a requested date range.")
    public ApiResponse<RentalItemResponse> getById(
            @PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        return ApiResponse.ok(rentalItemService.getActiveById(id, startDate, endDate));
    }
}
