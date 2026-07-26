package com.maddiewest.rentalservice.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgreementAcknowledgmentDto {

    @AssertTrue(message = "The rental agreement must be acknowledged")
    private boolean acknowledged;

    @NotBlank
    private String signatureName;

    @NotBlank
    private String agreementVersion;
}
