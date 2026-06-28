package com.maddiewest.rentalservice;

import com.maddiewest.rentalservice.dto.request.LoginRequest;
import com.maddiewest.rentalservice.dto.request.RentalDateRangeDto;
import com.maddiewest.rentalservice.dto.request.RentalItemCreateRequest;
import com.maddiewest.rentalservice.dto.request.RentalRequestCreateRequest;
import com.maddiewest.rentalservice.dto.request.RentalRequestLineItemDto;
import com.maddiewest.rentalservice.dto.request.RequesterInfoDto;
import com.maddiewest.rentalservice.dto.response.ApiResponse;
import com.maddiewest.rentalservice.dto.response.LoginResponse;
import com.maddiewest.rentalservice.dto.response.RentalItemResponse;
import com.maddiewest.rentalservice.dto.response.RentalRequestResponse;
import com.maddiewest.rentalservice.document.RentalRequestStatus;
import com.maddiewest.rentalservice.exception.AvailabilityConflictException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end lifecycle test: create an item, submit a rental request, approve it
 * (availability drops), attempt to over-approve a conflicting request (409), then
 * cancel the approved request and confirm availability is restored.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RentalLifecycleIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongoDBContainer.getReplicaSetUrl("rental-service-test"));
    }

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    private String authToken;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeAll
    void login() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testadmin");
        loginRequest.setPassword("testpass123");

        ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.exchange(
                baseUrl() + "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        authToken = response.getBody().getData().getToken();
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

        // 7. Cancel the first (approved) request, which should restore availability
        ResponseEntity<ApiResponse<RentalRequestResponse>> cancelResponse = restTemplate.exchange(
                baseUrl() + "/api/admin/rental-requests/" + requestId + "/cancel",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {
                });
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelResponse.getBody().getData().getStatus()).isEqualTo(RentalRequestStatus.CANCELLED);

        // 8. Availability is restored
        assertThat(availableQuantity(itemId, startDate, endDate)).isEqualTo(2);
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

        ResponseEntity<ApiResponse<RentalRequestResponse>> response = restTemplate.exchange(
                baseUrl() + "/api/rental-requests",
                HttpMethod.POST,
                new HttpEntity<>(createRequest),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().getData().getId();
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
