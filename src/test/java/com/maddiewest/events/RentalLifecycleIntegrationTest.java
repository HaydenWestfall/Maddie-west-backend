package com.maddiewest.events;

import com.maddiewest.events.dto.request.AgreementAcknowledgmentDto;
import com.maddiewest.events.dto.request.RentalDateRangeDto;
import com.maddiewest.events.dto.request.RentalItemCreateRequest;
import com.maddiewest.events.dto.request.RentalRequestCreateRequest;
import com.maddiewest.events.dto.request.RentalRequestLineItemDto;
import com.maddiewest.events.dto.request.RequesterInfoDto;
import com.maddiewest.events.dto.response.ApiResponse;
import com.maddiewest.events.dto.response.RentalItemResponse;
import com.maddiewest.events.dto.response.RentalRequestResponse;
import com.maddiewest.events.document.AgreementAcknowledgment;
import com.maddiewest.events.document.CoordinatorUser;
import com.maddiewest.events.document.RentalRequestStatus;
import com.maddiewest.events.exception.AvailabilityConflictException;
import com.maddiewest.events.repository.CoordinatorUserRepository;
import com.maddiewest.events.security.JwtService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MongoDBContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end lifecycle test: create an item, submit a rental request, approve it
 * (availability drops), attempt to over-approve a conflicting request (409), then
 * mark the approved request as paid and confirm the items remain reserved.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RentalLifecycleIntegrationTest {

    // Started in a static initializer rather than through @Testcontainers/@Container: under the
    // PER_CLASS lifecycle JUnit creates the test instance (which loads the Spring context, and with
    // it @DynamicPropertySource) before the Testcontainers extension's beforeAll callback runs, so
    // the container would still be stopped when the Mongo URI is resolved. Ryuk stops it at exit.
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    static {
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongoDBContainer.getReplicaSetUrl("rental-service-test"));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CoordinatorUserRepository coordinatorUserRepository;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    private String authToken;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeAll
    void login() {
        // Auth is Google-only in production; for tests we seed a coordinator
        // directly and mint a JWT rather than verifying a real Google ID token.
        CoordinatorUser user = new CoordinatorUser();
        user.setEmail("testadmin@example.com");
        user.setName("Test Admin");
        user.setRole("ADMIN");
        coordinatorUserRepository.save(user);

        authToken = jwtService.generateToken(user.getEmail(), user.getRole());
        assertThat(authToken).isNotBlank();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        return headers;
    }

    @Test
    void fullRentalRequestLifecycle() {
        // 1. Create a rental item with 2 units of stock
        RentalItemCreateRequest createItem = new RentalItemCreateRequest();
        createItem.setName("Integration Test Tablecloth");
        createItem.setDescription("White linen tablecloth");
        createItem.setCategory("Linens");
        createItem.setTotalQuantity(2);
        createItem.setPrice(new BigDecimal("15.00"));

        ResponseEntity<ApiResponse<RentalItemResponse>> createItemResponse = restTemplate.exchange(
                baseUrl() + "/api/admin/items",
                HttpMethod.POST,
                new HttpEntity<>(createItem, authHeaders()),
                new ParameterizedTypeReference<>() {
                });
        assertThat(createItemResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String itemId = createItemResponse.getBody().getData().getId();
        assertThat(itemId).isNotBlank();

        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 3);

        // 2. Submit a rental request for both units
        String requestId = submitRequest(itemId, 2, startDate, endDate);

        // 3. Before approval, all units are still available
        assertThat(availableQuantity(itemId, startDate, endDate)).isEqualTo(2);

        // 4. Approve the request
        ResponseEntity<ApiResponse<RentalRequestResponse>> approveResponse = restTemplate.exchange(
                baseUrl() + "/api/admin/rental-requests/" + requestId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {
                });
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approveResponse.getBody().getData().getStatus()).isEqualTo(RentalRequestStatus.APPROVED);

        // 5. After approval, availability drops to zero for the overlapping range
        assertThat(availableQuantity(itemId, startDate, endDate)).isEqualTo(0);

        // 6. A second overlapping request for 1 unit cannot be approved (409 conflict)
        String secondRequestId = submitRequest(itemId, 1, startDate, endDate);

        ResponseEntity<ApiResponse<List<AvailabilityConflictException.ConflictDetail>>> conflictResponse = restTemplate.exchange(
                baseUrl() + "/api/admin/rental-requests/" + secondRequestId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {
                });
        assertThat(conflictResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflictResponse.getBody().getData()).hasSize(1);
        assertThat(conflictResponse.getBody().getData().get(0).itemId()).isEqualTo(itemId);
        assertThat(conflictResponse.getBody().getData().get(0).availableQuantity()).isEqualTo(0);

        // 7. Mark the first (approved) request as paid once the Venmo payment clears
        ResponseEntity<ApiResponse<RentalRequestResponse>> markPaidResponse = restTemplate.exchange(
                baseUrl() + "/api/admin/rental-requests/" + requestId + "/mark-paid",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {
                });
        assertThat(markPaidResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(markPaidResponse.getBody().getData().getStatus()).isEqualTo(RentalRequestStatus.PAID);

        // 8. A paid request still reserves the items, so availability stays at zero
        assertThat(availableQuantity(itemId, startDate, endDate)).isEqualTo(0);
    }

    private String submitRequest(String itemId, int quantity, LocalDate startDate, LocalDate endDate) {
        RentalRequestLineItemDto lineItem = new RentalRequestLineItemDto();
        lineItem.setItemId(itemId);
        lineItem.setQuantity(quantity);

        RentalDateRangeDto dateRange = new RentalDateRangeDto();
        dateRange.setStartDate(startDate);
        dateRange.setEndDate(endDate);

        RequesterInfoDto requester = new RequesterInfoDto();
        requester.setName("Test Requester");
        requester.setEmail("requester@example.com");
        requester.setPhone("555-1234");

        RentalRequestCreateRequest createRequest = new RentalRequestCreateRequest();
        createRequest.setItems(List.of(lineItem));
        createRequest.setDateRange(dateRange);
        createRequest.setRequester(requester);
        createRequest.setAgreement(acknowledgedAgreement());

        ResponseEntity<ApiResponse<RentalRequestResponse>> response = restTemplate.exchange(
                baseUrl() + "/api/rental-requests",
                HttpMethod.POST,
                new HttpEntity<>(createRequest),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // The rental agreement acknowledgment is captured at submission and stamped server-side.
        AgreementAcknowledgment agreement = response.getBody().getData().getAgreement();
        assertThat(agreement).isNotNull();
        assertThat(agreement.isAcknowledged()).isTrue();
        assertThat(agreement.getSignatureName()).isEqualTo("Test Requester");
        assertThat(agreement.getAgreementVersion()).isEqualTo("2026-07-01");
        assertThat(agreement.getAcknowledgedAt()).isNotNull();

        return response.getBody().getData().getId();
    }

    private AgreementAcknowledgmentDto acknowledgedAgreement() {
        AgreementAcknowledgmentDto agreement = new AgreementAcknowledgmentDto();
        agreement.setAcknowledged(true);
        agreement.setSignatureName("Test Requester");
        agreement.setAgreementVersion("2026-07-01");
        return agreement;
    }

    @Test
    void submitWithoutAgreementIsRejected() {
        RentalRequestLineItemDto lineItem = new RentalRequestLineItemDto();
        lineItem.setItemId("000000000000000000000000");
        lineItem.setQuantity(1);

        RentalDateRangeDto dateRange = new RentalDateRangeDto();
        dateRange.setStartDate(LocalDate.of(2026, 8, 1));
        dateRange.setEndDate(LocalDate.of(2026, 8, 2));

        RequesterInfoDto requester = new RequesterInfoDto();
        requester.setName("No Agreement");
        requester.setEmail("noagreement@example.com");

        RentalRequestCreateRequest createRequest = new RentalRequestCreateRequest();
        createRequest.setItems(List.of(lineItem));
        createRequest.setDateRange(dateRange);
        createRequest.setRequester(requester);
        // No agreement set at all -> @NotNull violation -> 400. GlobalExceptionHandler answers
        // validation failures with ApiResponse<Map<field, message>>, not a RentalRequestResponse.
        ResponseEntity<ApiResponse<Map<String, String>>> response = restTemplate.exchange(
                baseUrl() + "/api/rental-requests",
                HttpMethod.POST,
                new HttpEntity<>(createRequest),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getData()).containsKey("agreement");
    }

    private int availableQuantity(String itemId, LocalDate startDate, LocalDate endDate) {
        ResponseEntity<ApiResponse<List<RentalItemResponse>>> response = restTemplate.exchange(
                baseUrl() + "/api/items/available?startDate=" + startDate + "&endDate=" + endDate,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().getData().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow()
                .getAvailableQuantity();
    }
}
