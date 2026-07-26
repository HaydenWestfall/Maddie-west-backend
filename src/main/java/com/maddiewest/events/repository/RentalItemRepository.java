package com.maddiewest.events.repository;

import com.maddiewest.events.document.RentalItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface RentalItemRepository extends MongoRepository<RentalItem, String> {

    List<RentalItem> findByIdIn(Collection<String> ids);
}
