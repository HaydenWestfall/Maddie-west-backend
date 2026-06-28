package com.maddiewest.rentalservice.controller;

import com.maddiewest.rentalservice.document.RentalRequestStatus;
import com.maddiewest.rentalservice.dto.request.RejectRequestDto;
import com.maddiewest.rentalservice.dto.response.ApiResponse;
import com.maddiewest.rentalservice.dto.response.PaginationMeta;
import com.maddiewest.rentalservice.dto.response.RentalRequestResponse;
import com.maddiewest.rentalservice.service.RentalRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rental-requests")
@RequiredArgsConstructor
@Tag(name = "Admin requests", description = "Review and approve rental requests")
public class AdminRentalRequestController {

    private final RentalRequestService rentalRequestService;

    @GetMapping
    @Operation(summary = "List rental requests", description = "Returns rental requests optionally filtered by status.")
    public ApiResponse<List<RentalRequestResponse>> list(
            @RequestParam(required = false) RentalRequestStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Page<RentalRequestResponse> result = rentalRequestService.browse(
                status, PageRequest.of(Math.max(page - 1, 0), limit));
        return ApiResponse.ok(result.getContent(), PaginationMeta.of(page, limit, result.getTotalElements()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a rental request by id", description = "Retrieves a single rental request with its current status and history.")
    public ApiResponse<RentalRequestResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(rentalRequestService.getById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a rental request", description = "Approves the request and updates its workflow state.")
    public ApiResponse<RentalRequestResponse> approve(@PathVariable String id, Authentication authentication) {
        return ApiResponse.ok(rentalRequestService.approve(id, authentication.getName()));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a rental request", description = "Rejects the request and records the coordinator's reason.")
    public ApiResponse<RentalRequestResponse> reject(@PathVariable String id,
                                                       @Valid @RequestBody RejectRequestDto request,
                                                       Authentication authentication) {
        return ApiResponse.ok(rentalRequestService.reject(id, request.getReason(), authentication.getName()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a rental request", description = "Cancels a rental request and records the change in history.")
    public ApiResponse<RentalRequestResponse> cancel(@PathVariable String id, Authentication authentication) {
        return ApiResponse.ok(rentalRequestService.cancel(id, authentication.getName()));
    }
}
