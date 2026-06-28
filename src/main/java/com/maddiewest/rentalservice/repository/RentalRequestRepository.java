package com.maddiewest.rentalservice.repository;

import com.maddiewest.rentalservice.document.RentalRequest;
import com.maddiewest.rentalservice.document.RentalRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RentalRequestRepository extends MongoRepository<RentalRequest, String> {

    Page<RentalRequest> findByStatus(RentalRequestStatus status, Pageable pageable);
}
