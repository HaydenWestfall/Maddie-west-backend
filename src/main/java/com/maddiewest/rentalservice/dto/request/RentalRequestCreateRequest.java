package com.maddiewest.rentalservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RentalRequestCreateRequest {

    @NotEmpty
    @Valid
    private List<RentalRequestLineItemDto> items;

    @NotNull
    @Valid
    private RentalDateRangeDto dateRange;

    @NotNull
    @Valid
    private RequesterInfoDto requester;

    @NotNull
    @Valid
    private AgreementAcknowledgmentDto agreement;
}
