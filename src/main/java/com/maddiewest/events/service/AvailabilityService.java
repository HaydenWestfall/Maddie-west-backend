package com.maddiewest.rentalservice.service;

import com.maddiewest.rentalservice.document.RentalItem;
import com.maddiewest.rentalservice.document.RentalRequestStatus;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes rental item availability for a given date range based on quantities
 * already reserved by APPROVED or PAID rental requests whose date range overlaps it.
 */
@Service
public class AvailabilityService {

    private final MongoTemplate mongoTemplate;

    public AvailabilityService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Sum of quantities reserved by APPROVED or PAID requests overlapping [start, end], keyed by
     * itemId. Items with no reservations in the range are simply absent from the map.
     */
    public Map<String, Integer> getReservedQuantities(LocalDate start, LocalDate end) {
        return getReservedQuantities(start, end, null);
    }

    public Map<String, Integer> getReservedQuantities(LocalDate start, LocalDate end, Collection<String> itemIds) {
        Criteria matchCriteria = Criteria.where("status")
                .in(RentalRequestStatus.APPROVED, RentalRequestStatus.PAID)
                .and("dateRange.startDate").lte(end)
                .and("dateRange.endDate").gte(start);

        var operations = new java.util.ArrayList<org.springframework.data.mongodb.core.aggregation.AggregationOperation>();
        operations.add(Aggregation.match(matchCriteria));
        operations.add(Aggregation.unwind("items"));
        if (itemIds != null && !itemIds.isEmpty()) {
            operations.add(Aggregation.match(Criteria.where("items.itemId").in(itemIds)));
        }
        operations.add(Aggregation.group("items.itemId").sum("items.quantity").as("reservedQuantity"));

        Aggregation aggregation = Aggregation.newAggregation(operations);
        AggregationResults<ReservedQuantity> results =
                mongoTemplate.aggregate(aggregation, "rental_requests", ReservedQuantity.class);

        return results.getMappedResults().stream()
                .collect(Collectors.toMap(ReservedQuantity::getId, ReservedQuantity::getReservedQuantity));
    }

    /**
     * Available quantity for a single item over [start, end].
     */
    public int getAvailableQuantity(RentalItem item, LocalDate start, LocalDate end) {
        int reserved = getReservedQuantities(start, end, java.util.List.of(item.getId()))
                .getOrDefault(item.getId(), 0);
        return Math.max(item.getTotalQuantity() - reserved, 0);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    private static class ReservedQuantity {
        @Id
        private String id;
        private int reservedQuantity;

        public String getId() {
            return id;
        }

        public int getReservedQuantity() {
            return reservedQuantity;
        }
    }
}
