package com.maddiewest.events.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.maddiewest.events.document.CoordinatorUser;
import com.maddiewest.events.dto.request.GoogleLoginRequest;
import com.maddiewest.events.dto.response.LoginResponse;
import com.maddiewest.events.exception.AdminAccessDeniedException;
import com.maddiewest.events.repository.CoordinatorUserRepository;
import com.maddiewest.events.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.time.Instant;

@Slf4j
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
            log.warn("Google token verification failed: {}", ex.getMessage());
            throw new BadCredentialsException("Invalid Google token");
        }

        if (idToken == null) {
            log.warn("Google token verification returned no payload");
            throw new BadCredentialsException("Invalid Google token");
        }

        String email = idToken.getPayload().getEmail();

        CoordinatorUser user = coordinatorUserRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login denied for {}: no coordinator account found", email);
                    return new AdminAccessDeniedException("Access denied. Admin account not found.");
                });

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        Instant expiresAt = jwtService.getExpiration(token).toInstant();

        log.info("Coordinator {} logged in successfully (role={})", user.getEmail(), user.getRole());

        return LoginResponse.builder()
                .token(token)
                .expiresAt(expiresAt)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
