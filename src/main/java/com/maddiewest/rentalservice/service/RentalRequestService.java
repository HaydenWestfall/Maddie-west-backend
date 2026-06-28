package com.maddiewest.rentalservice.service;

import com.maddiewest.rentalservice.document.RentalDateRange;
import com.maddiewest.rentalservice.document.RentalItem;
import com.maddiewest.rentalservice.document.RentalRequest;
import com.maddiewest.rentalservice.document.RentalRequestLineItem;
import com.maddiewest.rentalservice.document.RentalRequestStatus;
import com.maddiewest.rentalservice.document.RequesterInfo;
import com.maddiewest.rentalservice.document.StatusHistoryEntry;
import com.maddiewest.rentalservice.dto.request.RentalRequestCreateRequest;
import com.maddiewest.rentalservice.dto.request.RentalRequestLineItemDto;
import com.maddiewest.rentalservice.dto.response.RentalRequestResponse;
import com.maddiewest.rentalservice.exception.AvailabilityConflictException;
import com.maddiewest.rentalservice.exception.InvalidStatusTransitionException;
import com.maddiewest.rentalservice.exception.ResourceNotFoundException;
import com.maddiewest.rentalservice.mapper.RentalRequestMapper;
import com.maddiewest.rentalservice.repository.RentalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentalRequestService {

    private final RentalRequestRepository rentalRequestRepository;
    private final RentalItemService rentalItemService;
    private final AvailabilityService availabilityService;
    private final RentalRequestMapper rentalRequestMapper;
    private final EmailNotificationService emailNotificationService;

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

    public Page<RentalRequestResponse> browse(RentalRequestStatus status, Pageable pageable) {
        Page<RentalRequest> page = status != null
                ? rentalRequestRepository.findByStatus(status, pageable)
                : rentalRequestRepository.findAll(pageable);
        return page.map(rentalRequestMapper::toResponse);
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
     * Cancels a PENDING or APPROVED request. Cancelling an APPROVED request restores
     * availability automatically, since CANCELLED requests are excluded from the
     * reserved-quantity aggregation.
     */
    public RentalRequestResponse cancel(String id, String changedBy) {
        RentalRequest request = findEntity(id);
        if (request.getStatus() != RentalRequestStatus.PENDING && request.getStatus() != RentalRequestStatus.APPROVED) {
            throw new InvalidStatusTransitionException(
                    "Cannot cancel a request with status " + request.getStatus());
        }

        request.setStatus(RentalRequestStatus.CANCELLED);
        request.getStatusHistory().add(
                new StatusHistoryEntry(RentalRequestStatus.CANCELLED, Instant.now(), changedBy, null));
        return rentalRequestMapper.toResponse(rentalRequestRepository.save(request));
    }

    private void requireStatus(RentalRequest request, RentalRequestStatus required, String action) {
        if (request.getStatus() != required) {
            throw new InvalidStatusTransitionException(
                    "Request must be " + required + " to be " + action + " (current status: " + request.getStatus() + ")");
        }
    }
}
