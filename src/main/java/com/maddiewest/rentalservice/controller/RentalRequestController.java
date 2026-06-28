package com.maddiewest.rentalservice.controller;

import com.maddiewest.rentalservice.dto.request.RentalRequestCreateRequest;
import com.maddiewest.rentalservice.dto.response.ApiResponse;
import com.maddiewest.rentalservice.dto.response.RentalRequestResponse;
import com.maddiewest.rentalservice.service.RentalRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rental-requests")
@RequiredArgsConstructor
@Tag(name = "Rental requests", description = "Submit and manage rental requests")
public class RentalRequestController {

    private final RentalRequestService rentalRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a rental request", description = "Creates a new rental request for one or more items over a requested date range.")
    public ApiResponse<RentalRequestResponse> submit(@Valid @RequestBody RentalRequestCreateRequest request) {
        return ApiResponse.ok(rentalRequestService.submit(request));
    }
}
