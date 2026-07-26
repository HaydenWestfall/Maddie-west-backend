package com.maddiewest.events.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RentalDateRangeDto {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
