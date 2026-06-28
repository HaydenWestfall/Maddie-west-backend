package com.maddiewest.rentalservice.service;

import com.maddiewest.rentalservice.dto.response.RentalRequestResponse;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Sends transactional emails for the rental request lifecycle. All sends are
 * asynchronous and failures are logged rather than propagated, so SMTP issues
 * never fail the underlying API request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.coordinator-email}")
    private String coordinatorEmail;

    @Async
    public void notifyCoordinatorOfNewRequest(RentalRequestResponse request) {
        Context context = new Context();
        context.setVariable("request", request);
        sendHtmlEmail(coordinatorEmail, "New rental request awaiting review", "email/new-request-coordinator", context);
    }

    @Async
    public void sendRequestConfirmation(RentalRequestResponse request) {
        Context context = new Context();
        context.setVariable("request", request);
        sendHtmlEmail(request.getRequester().getEmail(), "We received your rental request",
                "email/request-confirmation-customer", context);
    }

    @Async
    public void sendApprovalNotification(RentalRequestResponse request) {
        Context context = new Context();
        context.setVariable("request", request);
        sendHtmlEmail(request.getRequester().getEmail(), "Your rental request has been approved",
                "email/request-approved-customer", context);
    }

    @Async
    public void sendRejectionNotification(RentalRequestResponse request, String reason) {
        Context context = new Context();
        context.setVariable("request", request);
        context.setVariable("reason", reason);
        sendHtmlEmail(request.getRequester().getEmail(), "Update on your rental request",
                "email/request-rejected-customer", context);
    }

    private void sendHtmlEmail(String to, String subject, String template, Context context) {
        try {
            String html = templateEngine.process(template, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send '{}' email to {}", subject, to, ex);
        }
    }
}
