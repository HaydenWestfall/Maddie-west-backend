package com.maddiewest.events.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgreementAcknowledgment {

    private boolean acknowledged;
    private String signatureName;
    private String agreementVersion;
    private Instant acknowledgedAt;
}
