package com.maddiewest.rentalservice.controller;

import com.maddiewest.rentalservice.dto.request.ContactFormRequest;
import com.maddiewest.rentalservice.dto.response.ContactFormResponse;
import com.maddiewest.rentalservice.exception.RateLimitExceededException;
import com.maddiewest.rentalservice.service.ContactRateLimiter;
import com.maddiewest.rentalservice.service.EmailNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public contact form for the marketing site. Absorbs the job previously handled by the
 * standalone contact-api for maddiewestevents.com. Emails the coordinator; does not persist.
 */
@Slf4j
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Tag(name = "Contact", description = "Public website contact form")
public class ContactController {

    private final EmailNotificationService emailNotificationService;
    private final ContactRateLimiter rateLimiter;

    @PostMapping
    @Operation(summary = "Submit a contact form",
            description = "Sends the submission to the coordinator by email. Rate limited per IP.")
    public ContactFormResponse submit(@Valid @RequestBody ContactFormRequest request,
                                      HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        if (!rateLimiter.tryAcquire(ip)) {
            log.warn("Contact form rate limit exceeded for IP {}", ip);
            throw new RateLimitExceededException("Too many requests from this IP, please try again later.");
        }
        emailNotificationService.sendContactFormNotification(request);
        return ContactFormResponse.ok("Your contact form submission has been sent successfully!");
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
