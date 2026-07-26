package com.maddiewest.events.controller;

import com.maddiewest.events.dto.request.GoogleLoginRequest;
import com.maddiewest.events.dto.response.ApiResponse;
import com.maddiewest.events.dto.response.LoginResponse;
import com.maddiewest.events.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and login endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    @Operation(summary = "Authenticate via Google", description = "Verifies a Google ID token and returns a JWT for authorized coordinators.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication succeeded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid Google token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account is not an authorized coordinator")
    })
    public ApiResponse<LoginResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.ok(authService.loginWithGoogle(request));
    }
}
