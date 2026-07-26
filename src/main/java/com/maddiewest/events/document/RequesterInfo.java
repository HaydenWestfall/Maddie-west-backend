package com.maddiewest.rentalservice.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequesterInfo {

    private String name;
    private String email;
    private String phone;
    private String notes;
}
