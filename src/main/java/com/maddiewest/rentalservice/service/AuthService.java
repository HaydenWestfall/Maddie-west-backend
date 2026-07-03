package com.maddiewest.rentalservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.maddiewest.rentalservice.document.CoordinatorUser;
import com.maddiewest.rentalservice.dto.request.GoogleLoginRequest;
import com.maddiewest.rentalservice.dto.response.LoginResponse;
import com.maddiewest.rentalservice.exception.AdminAccessDeniedException;
import com.maddiewest.rentalservice.repository.CoordinatorUserRepository;
import com.maddiewest.rentalservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CoordinatorUserRepository coordinatorUserRepository;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdToken idToken;
        try {
            idToken = googleIdTokenVerifier.verify(request.getIdToken());
        } catch (GeneralSecurityException | java.io.IOException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid Google token");
        }

        if (idToken == null) {
            throw new BadCredentialsException("Invalid Google token");
        }

        String email = idToken.getPayload().getEmail();

        CoordinatorUser user = coordinatorUserRepository.findByEmail(email)
                .orElseThrow(() -> new AdminAccessDeniedException("Access denied. Admin account not found."));

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        Instant expiresAt = jwtService.getExpiration(token).toInstant();

        return LoginResponse.builder()
                .token(token)
                .expiresAt(expiresAt)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
