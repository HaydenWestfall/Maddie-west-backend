package com.maddiewest.rentalservice.repository;

import com.maddiewest.rentalservice.document.RentalRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RentalRequestRepository extends MongoRepository<RentalRequest, String> {
}
