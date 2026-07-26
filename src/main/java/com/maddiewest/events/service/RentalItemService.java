package com.maddiewest.rentalservice.service;

import com.maddiewest.rentalservice.document.RentalItem;
import com.maddiewest.rentalservice.dto.request.RentalItemCreateRequest;
import com.maddiewest.rentalservice.dto.request.RentalItemUpdateRequest;
import com.maddiewest.rentalservice.dto.response.RentalItemResponse;
import com.maddiewest.rentalservice.exception.ResourceNotFoundException;
import com.maddiewest.rentalservice.mapper.RentalItemMapper;
import com.maddiewest.rentalservice.repository.RentalItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentalItemService {

    private final RentalItemRepository rentalItemRepository;
    private final MongoTemplate mongoTemplate;
    private final RentalItemMapper rentalItemMapper;
    private final AvailabilityService availabilityService;

    /**
     * Browse active rental items, optionally filtered by category/name. Used by the
     * public {@code GET /api/items} endpoint. {@code availableQuantity} defaults to
     * {@code totalQuantity} since no date range is considered here.
     */
    public Page<RentalItemResponse> browseActive(String category, String name, Pageable pageable) {
        Query query = buildActiveQuery(category, name);
        long total = mongoTemplate.count(query, RentalItem.class);
        List<RentalItem> items = mongoTemplate.find(query.with(pageable), RentalItem.class);
        List<RentalItemResponse> responses = items.stream()
                .map(rentalItemMapper::toResponse)
                .toList();
        return new PageImpl<>(responses, pageable, total);
    }

    public RentalItemResponse getActiveById(String id) {
        RentalItem item = findActiveEntity(id);
        return rentalItemMapper.toResponse(item);
    }

    /**
     * Same as {@link #getActiveById(String)} but computes {@code availableQuantity}
     * against the given date range when both dates are supplied.
     */
    public RentalItemResponse getActiveById(String id, LocalDate startDate, LocalDate endDate) {
        RentalItem item = findActiveEntity(id);
        if (startDate == null || endDate == null) {
            return rentalItemMapper.toResponse(item);
        }
        int availableQuantity = availabilityService.getAvailableQuantity(item, startDate, endDate);
        return rentalItemMapper.toResponse(item, availableQuantity);
    }

    /**
     * Browse active rental items with {@code availableQuantity} computed for the given
     * date range. Used by the public {@code GET /api/items/available} endpoint.
     */
    public Page<RentalItemResponse> browseAvailable(String category, String name, LocalDate startDate,
                                                      LocalDate endDate, Pageable pageable) {
        Query query = buildActiveQuery(category, name);
        long total = mongoTemplate.count(query, RentalItem.class);
        List<RentalItem> items = mongoTemplate.find(query.with(pageable), RentalItem.class);

        Map<String, Integer> reserved = availabilityService.getReservedQuantities(startDate, endDate);
        List<RentalItemResponse> responses = items.stream()
                .map(item -> rentalItemMapper.toResponse(
                        item, Math.max(item.getTotalQuantity() - reserved.getOrDefault(item.getId(), 0), 0)))
                .toList();
        return new PageImpl<>(responses, pageable, total);
    }

    public RentalItem findActiveEntity(String id) {
        return rentalItemRepository.findById(id)
                .filter(RentalItem::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Rental item not found: " + id));
    }

    public RentalItem findEntity(String id) {
        return rentalItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental item not found: " + id));
    }

    public List<RentalItem> findEntitiesByIds(List<String> ids) {
        return rentalItemRepository.findByIdIn(ids);
    }

    // --- Admin operations ---

    public Page<RentalItemResponse> browseAll(Pageable pageable) {
        long total = rentalItemRepository.count();
        List<RentalItemResponse> responses = mongoTemplate.find(new Query().with(pageable), RentalItem.class)
                .stream()
                .map(rentalItemMapper::toResponse)
                .toList();
        return new PageImpl<>(responses, pageable, total);
    }

    public RentalItemResponse getById(String id) {
        return rentalItemMapper.toResponse(findEntity(id));
    }

    public RentalItemResponse create(RentalItemCreateRequest request) {
        RentalItem item = rentalItemMapper.toDocument(request);
        RentalItem saved = rentalItemRepository.save(item);
        log.info("Created rental item {} ({})", saved.getId(), saved.getName());
        return rentalItemMapper.toResponse(saved);
    }

    public RentalItemResponse update(String id, RentalItemUpdateRequest request) {
        RentalItem item = findEntity(id);
        rentalItemMapper.updateDocument(item, request);
        RentalItem saved = rentalItemRepository.save(item);
        log.info("Updated rental item {} ({})", saved.getId(), saved.getName());
        return rentalItemMapper.toResponse(saved);
    }

    public void softDelete(String id) {
        RentalItem item = findEntity(id);
        item.setActive(false);
        rentalItemRepository.save(item);
        log.info("Deactivated rental item {} ({})", item.getId(), item.getName());
    }

    private Query buildActiveQuery(String category, String name) {
        Criteria criteria = Criteria.where("active").is(true);
        if (StringUtils.hasText(category)) {
            criteria.and("category").regex("^" + Pattern.quote(category) + "$", "i");
        }
        if (StringUtils.hasText(name)) {
            criteria.and("name").regex(Pattern.quote(name), "i");
        }
        return new Query(criteria);
    }
}
