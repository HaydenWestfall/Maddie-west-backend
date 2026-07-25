package com.maddiewest.rentalservice.service;

import com.maddiewest.rentalservice.document.AgreementAcknowledgment;
import com.maddiewest.rentalservice.document.RentalDateRange;
import com.maddiewest.rentalservice.document.RentalItem;
import com.maddiewest.rentalservice.document.RentalRequest;
import com.maddiewest.rentalservice.document.RentalRequestLineItem;
import com.maddiewest.rentalservice.document.RentalRequestStatus;
import com.maddiewest.rentalservice.document.RequesterInfo;
import com.maddiewest.rentalservice.document.StatusHistoryEntry;
import com.maddiewest.rentalservice.dto.request.RentalRequestCreateRequest;
import com.maddiewest.rentalservice.dto.request.RentalRequestLineItemDto;
import com.maddiewest.rentalservice.dto.request.RentalRequestSearchCriteria;
import com.maddiewest.rentalservice.dto.response.RentalRequestResponse;
import com.maddiewest.rentalservice.exception.AvailabilityConflictException;
import com.maddiewest.rentalservice.exception.InvalidStatusTransitionException;
import com.maddiewest.rentalservice.exception.ResourceNotFoundException;
import com.maddiewest.rentalservice.mapper.RentalRequestMapper;
import com.maddiewest.rentalservice.repository.RentalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentalRequestService {

    private final RentalRequestRepository rentalRequestRepository;
    private final RentalItemService rentalItemService;
    private final AvailabilityService availabilityService;
    private final RentalRequestMapper rentalRequestMapper;
    private final EmailNotificationService emailNotificationService;
    private final MongoTemplate mongoTemplate;

    /** Maps UI sort keys to the underlying (stored or computed) document fields. */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "customer", "requester.name",
            "email", "requester.email",
            "eventDate", "dateRange.startDate",
            "items", "itemCount",
            "total", "totalPrice",
            "status", "status",
            "submitted", "createdAt");

    public RentalRequestResponse submit(RentalRequestCreateRequest request) {
        if (request.getDateRange().getEndDate().isBefore(request.getDateRange().getStartDate())) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }

        List<String> itemIds = request.getItems().stream()
                .map(RentalRequestLineItemDto::getItemId)
                .toList();
        Map<String, RentalItem> itemsById = rentalItemService.findEntitiesByIds(itemIds).stream()
                .collect(Collectors.toMap(RentalItem::getId, Function.identity()));

        List<RentalRequestLineItem> lineItems = request.getItems().stream()
                .map(dto -> toLineItem(dto, itemsById))
                .toList();

        RentalRequest rentalRequest = new RentalRequest();
        rentalRequest.setItems(lineItems);
        rentalRequest.setDateRange(new RentalDateRange(
                request.getDateRange().getStartDate(), request.getDateRange().getEndDate()));
        rentalRequest.setRequester(new RequesterInfo(
                request.getRequester().getName(),
                request.getRequester().getEmail(),
                request.getRequester().getPhone(),
                request.getRequester().getNotes()));
        rentalRequest.setAgreement(new AgreementAcknowledgment(
                true,
                request.getAgreement().getSignatureName().trim(),
                request.getAgreement().getAgreementVersion(),
                Instant.now()));
        rentalRequest.setStatus(RentalRequestStatus.PENDING);
        rentalRequest.getStatusHistory().add(
                new StatusHistoryEntry(RentalRequestStatus.PENDING, Instant.now(), "system", null));

        RentalRequest saved = rentalRequestRepository.save(rentalRequest);
        RentalRequestResponse response = rentalRequestMapper.toResponse(saved);

        emailNotificationService.notifyCoordinatorOfNewRequest(response);
        emailNotificationService.sendRequestConfirmation(response);

        return response;
    }

    private RentalRequestLineItem toLineItem(RentalRequestLineItemDto dto, Map<String, RentalItem> itemsById) {
        RentalItem item = itemsById.get(dto.getItemId());
        if (item == null) {
            throw new ResourceNotFoundException("Rental item not found: " + dto.getItemId());
        }
        return new RentalRequestLineItem(item.getId(), item.getName(), dto.getQuantity(), item.getPrice());
    }

    // --- Admin operations ---

    /**
     * Filters, sorts and paginates rental requests entirely in the database. Sorting by
     * item count or total price relies on fields computed on the fly (the line-item total
     * is not stored), so results come from an aggregation rather than a plain find.
     */
    public Page<RentalRequestResponse> browse(RentalRequestSearchCriteria criteria, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(limit, 1), resolveSort(criteria));
        Criteria filter = buildFilter(criteria);

        long total = mongoTemplate.count(new Query(filter), RentalRequest.class);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<AggregationOperation> stages = List.of(
                Aggregation.match(filter),
                addComputedFields(),
                sortStage(pageable.getSort()),
                Aggregation.skip(pageable.getOffset()),
                Aggregation.limit(pageable.getPageSize()));

        Aggregation aggregation = Aggregation.newAggregation(RentalRequest.class, stages)
                .withOptions(AggregationOptions.builder()
                        .collation(Collation.of("en").strength(2))
                        .build());

        List<RentalRequest> results = mongoTemplate
                .aggregate(aggregation, RentalRequest.class, RentalRequest.class)
                .getMappedResults();

        return new PageImpl<>(results, pageable, total).map(rentalRequestMapper::toResponse);
    }

    private Sort resolveSort(RentalRequestSearchCriteria criteria) {
        String field = criteria.getSort() != null ? SORT_FIELDS.getOrDefault(criteria.getSort(), "createdAt") : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(criteria.getDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    private Criteria buildFilter(RentalRequestSearchCriteria f) {
        List<Criteria> conditions = new ArrayList<>();

        if (f.getStatus() != null) {
            conditions.add(Criteria.where("status").is(f.getStatus()));
        }
        if (StringUtils.hasText(f.getCustomer())) {
            conditions.add(Criteria.where("requester.name").regex(Pattern.quote(f.getCustomer().trim()), "i"));
        }
        if (StringUtils.hasText(f.getEmail())) {
            conditions.add(Criteria.where("requester.email").regex(Pattern.quote(f.getEmail().trim()), "i"));
        }
        if (f.getDateFrom() != null || f.getDateTo() != null) {
            Criteria dateCriteria = Criteria.where("dateRange.startDate");
            if (f.getDateFrom() != null) {
                dateCriteria.gte(f.getDateFrom().atStartOfDay(ZoneOffset.UTC).toInstant());
            }
            if (f.getDateTo() != null) {
                dateCriteria.lt(f.getDateTo().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
            }
            conditions.add(dateCriteria);
        }
        if (StringUtils.hasText(f.getSearch())) {
            String term = Pattern.quote(f.getSearch().trim());
            conditions.add(new Criteria().orOperator(
                    Criteria.where("requester.name").regex(term, "i"),
                    Criteria.where("requester.email").regex(term, "i"),
                    Criteria.where("status").regex(term, "i"),
                    Criteria.where("items.itemName").regex(term, "i")));
        }

        return conditions.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(conditions.toArray(new Criteria[0]));
    }

    /**
     * Adds {@code itemCount} and {@code totalPrice} so requests can be sorted by number of
     * items or overall value. {@code pricePerItem} is stored as a string (BigDecimal), so it
     * is converted to a decimal before the per-line multiply-and-sum.
     */
    private AggregationOperation addComputedFields() {
        Document itemCount = new Document("$size", new Document("$ifNull", List.of("$items", List.of())));
        Document totalPrice = new Document("$reduce", new Document()
                .append("input", new Document("$ifNull", List.of("$items", List.of())))
                .append("initialValue", new Decimal128(BigDecimal.ZERO))
                .append("in", new Document("$add", List.of(
                        "$$value",
                        new Document("$multiply", List.of(
                                new Document("$toDecimal", new Document("$ifNull", List.of("$$this.pricePerItem", "0"))),
                                new Document("$ifNull", List.of("$$this.quantity", 0))))))));
        return context -> new Document("$addFields", new Document()
                .append("itemCount", itemCount)
                .append("totalPrice", totalPrice));
    }

    private AggregationOperation sortStage(Sort sort) {
        Document sortDoc = new Document();
        for (Sort.Order order : sort) {
            sortDoc.append(order.getProperty(), order.isAscending() ? 1 : -1);
        }
        return context -> new Document("$sort", sortDoc);
    }

    public RentalRequestResponse getById(String id) {
        return rentalRequestMapper.toResponse(findEntity(id));
    }

    public RentalRequest findEntity(String id) {
        return rentalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental request not found: " + id));
    }

    /**
     * Approves a PENDING request after verifying enough stock remains for its date range.
     * Since the request is still PENDING at this point, it is automatically excluded from
     * the "reserved by APPROVED requests" aggregation used for the conflict check.
     */
    public RentalRequestResponse approve(String id, String changedBy) {
        RentalRequest request = findEntity(id);
        requireStatus(request, RentalRequestStatus.PENDING, "approved");

        Map<String, Integer> reserved = availabilityService.getReservedQuantities(
                request.getDateRange().getStartDate(), request.getDateRange().getEndDate());

        List<AvailabilityConflictException.ConflictDetail> conflicts = new ArrayList<>();
        for (RentalRequestLineItem lineItem : request.getItems()) {
            RentalItem item = rentalItemService.findEntity(lineItem.getItemId());
            int alreadyReserved = reserved.getOrDefault(lineItem.getItemId(), 0);
            int available = Math.max(item.getTotalQuantity() - alreadyReserved, 0);
            if (lineItem.getQuantity() > available) {
                conflicts.add(new AvailabilityConflictException.ConflictDetail(
                        lineItem.getItemId(), lineItem.getItemName(), lineItem.getQuantity(), available));
            }
        }

        if (!conflicts.isEmpty()) {
            throw new AvailabilityConflictException("Not enough availability to approve this request", conflicts);
        }

        request.setStatus(RentalRequestStatus.APPROVED);
        request.getStatusHistory().add(
                new StatusHistoryEntry(RentalRequestStatus.APPROVED, Instant.now(), changedBy, null));
        RentalRequestResponse response = rentalRequestMapper.toResponse(rentalRequestRepository.save(request));

        emailNotificationService.sendApprovalNotification(response);

        return response;
    }

    public RentalRequestResponse reject(String id, String reason, String changedBy) {
        RentalRequest request = findEntity(id);
        requireStatus(request, RentalRequestStatus.PENDING, "rejected");

        request.setStatus(RentalRequestStatus.REJECTED);
        request.getStatusHistory().add(
                new StatusHistoryEntry(RentalRequestStatus.REJECTED, Instant.now(), changedBy, reason));
        RentalRequestResponse response = rentalRequestMapper.toResponse(rentalRequestRepository.save(request));

        emailNotificationService.sendRejectionNotification(response, reason);

        return response;
    }

    /**
     * Marks an APPROVED request as PAID once the requester has completed their Venmo
     * payment. This is a manual confirmation by the coordinator. A PAID request continues
     * to reserve availability, since the items are still committed to the rental.
     */
    public RentalRequestResponse markPaid(String id, String changedBy) {
        RentalRequest request = findEntity(id);
        requireStatus(request, RentalRequestStatus.APPROVED, "marked paid");

        request.setStatus(RentalRequestStatus.PAID);
        request.getStatusHistory().add(
                new StatusHistoryEntry(RentalRequestStatus.PAID, Instant.now(), changedBy, null));
        return rentalRequestMapper.toResponse(rentalRequestRepository.save(request));
    }

    /**
     * Cancels an APPROVED or PAID request. Cancelling moves the request out of the
     * {@code APPROVED}/{@code PAID} set that {@link AvailabilityService} reserves against, so the
     * request's items are automatically freed for other rentals over its date range.
     */
    public RentalRequestResponse cancel(String id, String reason, String changedBy) {
        RentalRequest request = findEntity(id);
        requireStatusIn(request, "cancelled", RentalRequestStatus.APPROVED, RentalRequestStatus.PAID);

        request.setStatus(RentalRequestStatus.CANCELLED);
        request.getStatusHistory().add(
                new StatusHistoryEntry(RentalRequestStatus.CANCELLED, Instant.now(), changedBy, reason));
        RentalRequestResponse response = rentalRequestMapper.toResponse(rentalRequestRepository.save(request));

        emailNotificationService.sendCancellationNotification(response, reason);

        return response;
    }

    private void requireStatus(RentalRequest request, RentalRequestStatus required, String action) {
        if (request.getStatus() != required) {
            throw new InvalidStatusTransitionException(
                    "Request must be " + required + " to be " + action + " (current status: " + request.getStatus() + ")");
        }
    }

    private void requireStatusIn(RentalRequest request, String action, RentalRequestStatus... allowed) {
        if (!List.of(allowed).contains(request.getStatus())) {
            throw new InvalidStatusTransitionException(
                    "Request must be one of " + List.of(allowed) + " to be " + action
                            + " (current status: " + request.getStatus() + ")");
        }
    }
}
