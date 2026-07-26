package com.maddiewest.events.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequesterInfoDto {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    private String phone;

    private String notes;
}
