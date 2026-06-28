package com.maddiewest.rentalservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Instant expiresAt;
    private String username;
    private String role;
}
