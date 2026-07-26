package com.maddiewest.events.repository;

import com.maddiewest.events.document.RentalRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RentalRequestRepository extends MongoRepository<RentalRequest, String> {
}
