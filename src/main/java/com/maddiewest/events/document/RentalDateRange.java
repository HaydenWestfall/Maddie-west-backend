package com.maddiewest.events.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RentalDateRange {

    private LocalDate startDate;
    private LocalDate endDate;
}
