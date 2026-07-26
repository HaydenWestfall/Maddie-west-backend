package com.maddiewest.events.repository;

import com.maddiewest.events.document.CoordinatorUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CoordinatorUserRepository extends MongoRepository<CoordinatorUser, String> {

    Optional<CoordinatorUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
