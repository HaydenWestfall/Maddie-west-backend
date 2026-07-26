package com.maddiewest.events.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelRequestDto {

    /** Optional note recorded in the status history and shown to the customer. */
    private String reason;
}
