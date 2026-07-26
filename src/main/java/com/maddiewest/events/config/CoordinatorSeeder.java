package com.maddiewest.rentalservice.config;

import com.maddiewest.rentalservice.document.CoordinatorUser;
import com.maddiewest.rentalservice.repository.CoordinatorUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Idempotently seeds an initial coordinator/admin account from
 * {@code COORDINATOR_SEED_EMAIL} so the coordinator can sign in with that
 * Google account on first run. Does nothing if that email already exists
 * or the env var is unset.
 */
@Component
@RequiredArgsConstructor
public class CoordinatorSeeder implements CommandLineRunner {

    private final CoordinatorUserRepository coordinatorUserRepository;

    @Value("${app.coordinator-seed.email:}")
    private String seedEmail;

    @Value("${app.coordinator-seed.name:}")
    private String seedName;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(seedEmail) || coordinatorUserRepository.existsByEmail(seedEmail)) {
            return;
        }

        CoordinatorUser user = new CoordinatorUser();
        user.setEmail(seedEmail);
        user.setName(StringUtils.hasText(seedName) ? seedName : seedEmail);
        user.setRole("ADMIN");
        coordinatorUserRepository.save(user);
    }
}
