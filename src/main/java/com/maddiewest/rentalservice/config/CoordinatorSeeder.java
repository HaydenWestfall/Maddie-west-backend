package com.maddiewest.rentalservice.config;

import com.maddiewest.rentalservice.document.CoordinatorUser;
import com.maddiewest.rentalservice.repository.CoordinatorUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Idempotently seeds an initial coordinator/admin account from
 * {@code COORDINATOR_SEED_USERNAME} / {@code COORDINATOR_SEED_PASSWORD} so the
 * coordinator can log in on first run. Does nothing if that username already exists
 * or if either env var is unset.
 */
@Component
@RequiredArgsConstructor
public class CoordinatorSeeder implements CommandLineRunner {

    private final CoordinatorUserRepository coordinatorUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.coordinator-seed.username}")
    private String seedUsername;

    @Value("${app.coordinator-seed.password}")
    private String seedPassword;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(seedUsername) || !StringUtils.hasText(seedPassword)) {
            return;
        }

        if (coordinatorUserRepository.existsByUsername(seedUsername)) {
            return;
        }

        CoordinatorUser user = new CoordinatorUser();
        user.setUsername(seedUsername);
        user.setPasswordHash(passwordEncoder.encode(seedPassword));
        user.setRole("ADMIN");
        coordinatorUserRepository.save(user);
    }
}
