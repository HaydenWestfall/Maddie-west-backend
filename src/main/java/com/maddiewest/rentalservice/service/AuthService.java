package com.maddiewest.rentalservice.service;

import com.maddiewest.rentalservice.document.CoordinatorUser;
import com.maddiewest.rentalservice.dto.request.LoginRequest;
import com.maddiewest.rentalservice.dto.response.LoginResponse;
import com.maddiewest.rentalservice.repository.CoordinatorUserRepository;
import com.maddiewest.rentalservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CoordinatorUserRepository coordinatorUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        CoordinatorUser user = coordinatorUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        Instant expiresAt = jwtService.getExpiration(token).toInstant();

        return LoginResponse.builder()
                .token(token)
                .expiresAt(expiresAt)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}
