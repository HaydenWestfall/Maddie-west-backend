package com.maddiewest.events.controller;

import com.maddiewest.events.document.RentalRequestStatus;
import com.maddiewest.events.dto.request.CancelRequestDto;
import com.maddiewest.events.dto.request.RejectRequestDto;
import com.maddiewest.events.dto.request.RentalRequestSearchCriteria;
import com.maddiewest.events.dto.response.ApiResponse;
import com.maddiewest.events.dto.response.PaginationMeta;
import com.maddiewest.events.dto.response.RentalRequestResponse;
import com.maddiewest.events.service.RentalRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/rental-requests")
@RequiredArgsConstructor
@Tag(name = "Admin requests", description = "Review and approve rental requests")
public class AdminRentalRequestController {

    private final RentalRequestService rentalRequestService;

    @GetMapping
    @Operation(summary = "List rental requests",
            description = "Returns rental requests filtered, sorted and paginated server-side.")
    public ApiResponse<List<RentalRequestResponse>> list(
            @RequestParam(required = false) RentalRequestStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        RentalRequestSearchCriteria criteria = RentalRequestSearchCriteria.builder()
                .status(status)
                .search(search)
                .customer(customer)
                .email(email)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .sort(sort)
                .direction(dir)
                .build();
        Page<RentalRequestResponse> result = rentalRequestService.browse(criteria, page, limit);
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

    @PostMapping("/{id}/mark-paid")
    @Operation(summary = "Mark a rental request as paid", description = "Marks an approved request as paid once the requester has completed their Venmo payment.")
    public ApiResponse<RentalRequestResponse> markPaid(@PathVariable String id, Authentication authentication) {
        return ApiResponse.ok(rentalRequestService.markPaid(id, authentication.getName()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a rental request", description = "Cancels an approved or paid request, frees its reserved items, and notifies the customer.")
    public ApiResponse<RentalRequestResponse> cancel(@PathVariable String id,
                                                      @RequestBody(required = false) CancelRequestDto request,
                                                      Authentication authentication) {
        String reason = request != null ? request.getReason() : null;
        return ApiResponse.ok(rentalRequestService.cancel(id, reason, authentication.getName()));
    }
}
