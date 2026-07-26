package com.maddiewest.events.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maddiewest.events.dto.request.ContactFormRequest;
import com.maddiewest.events.exception.GlobalExceptionHandler;
import com.maddiewest.events.service.ContactRateLimiter;
import com.maddiewest.events.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc test for the public contact endpoint. Exercises the response contract,
 * bean validation, dynamic-field capture, and per-IP rate limiting without Mongo/security/SMTP.
 */
class ContactControllerTest {

    private MockMvc mockMvc;
    private EmailNotificationService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        emailService = mock(EmailNotificationService.class);
        ContactController controller = new ContactController(emailService, new ContactRateLimiter());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validSubmission_returnsSuccessContract_andEmailsCoordinator() throws Exception {
        String body = """
                {"name":"Jane Doe","email":"jane@example.com","phone":"555-1234",
                 "message":"Interested in an arbor","eventDate":"2026-09-01"}
                """;

        mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Your contact form submission has been sent successfully!"));

        // The arbitrary "eventDate" field must survive via @JsonAnySetter into additionalFields.
        ArgumentCaptor<ContactFormRequest> captor = ArgumentCaptor.forClass(ContactFormRequest.class);
        verify(emailService).sendContactFormNotification(captor.capture());
        ContactFormRequest captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Jane Doe");
        assertThat(captured.getAdditionalFields()).containsEntry("eventDate", "2026-09-01");
    }

    @Test
    void missingEmail_returnsValidationFailure() throws Exception {
        mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void exceedingRateLimit_returns429() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "Spammer", "email", "s@example.com"));

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }
}
