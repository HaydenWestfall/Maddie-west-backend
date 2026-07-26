package com.maddiewest.events.controller;

import com.maddiewest.events.dto.request.RentalItemCreateRequest;
import com.maddiewest.events.dto.request.RentalItemUpdateRequest;
import com.maddiewest.events.dto.response.ApiResponse;
import com.maddiewest.events.dto.response.PaginationMeta;
import com.maddiewest.events.dto.response.RentalItemResponse;
import com.maddiewest.events.service.RentalItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/items")
@RequiredArgsConstructor
@Tag(name = "Admin inventory", description = "Manage catalog items for coordinators and admins")
public class AdminRentalItemController {

    private final RentalItemService rentalItemService;

    @GetMapping
    @Operation(summary = "List all inventory items", description = "Returns the full inventory list including inactive items for admin review.")
    public ApiResponse<List<RentalItemResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Page<RentalItemResponse> result = rentalItemService.browseAll(PageRequest.of(Math.max(page - 1, 0), limit));
        return ApiResponse.ok(result.getContent(), PaginationMeta.of(page, limit, result.getTotalElements()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an inventory item by id", description = "Retrieves a single item record regardless of its active status.")
    public ApiResponse<RentalItemResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(rentalItemService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new inventory item", description = "Adds a new rental item to the catalog.")
    public ApiResponse<RentalItemResponse> create(@Valid @RequestBody RentalItemCreateRequest request) {
        return ApiResponse.ok(rentalItemService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an inventory item", description = "Updates the details and availability state of an existing inventory item.")
    public ApiResponse<RentalItemResponse> update(@PathVariable String id,
                                                    @Valid @RequestBody RentalItemUpdateRequest request) {
        return ApiResponse.ok(rentalItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an inventory item", description = "Marks an item inactive without removing it from the system.")
    public ApiResponse<Void> delete(@PathVariable String id) {
        rentalItemService.softDelete(id);
        return ApiResponse.ok(null);
    }
}
