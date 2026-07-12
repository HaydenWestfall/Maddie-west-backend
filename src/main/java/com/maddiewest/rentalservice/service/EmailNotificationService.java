package com.maddiewest.rentalservice.service;

import com.maddiewest.rentalservice.dto.request.ContactFormRequest;
import com.maddiewest.rentalservice.dto.response.RentalRequestResponse;
import jakarta.mail.internet.MimeMessage;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @Value("${app.payment.venmo-username}")
    private String venmoUsername;

    @Value("${app.admin.base-url}")
    private String adminBaseUrl;

    @Async
    public void notifyCoordinatorOfNewRequest(RentalRequestResponse request) {
        Context context = new Context();
        context.setVariable("request", request);
        context.setVariable("adminRequestUrl", buildAdminRequestUrl(request));
        // Reply-To the customer so the coordinator can respond to them directly from the notification.
        sendHtmlEmail(coordinatorEmail, request.getRequester().getEmail(),
                "New rental request awaiting review", "email/new-request-coordinator", context);
    }

    @Async
    public void sendRequestConfirmation(RentalRequestResponse request) {
        Context context = new Context();
        context.setVariable("request", request);
        // Reply-To the coordinator so a customer reply reaches a monitored inbox, not the no-reply address.
        sendHtmlEmail(request.getRequester().getEmail(), coordinatorEmail, "We received your rental request",
                "email/request-confirmation-customer", context);
    }

    @Async
    public void sendApprovalNotification(RentalRequestResponse request) {
        Context context = new Context();
        context.setVariable("request", request);
        context.setVariable("venmoUsername", venmoUsername);
        context.setVariable("venmoLink", buildVenmoPaymentLink(request));
        sendHtmlEmail(request.getRequester().getEmail(), coordinatorEmail, "Your rental request has been approved",
                "email/request-approved-customer", context);
    }

    /**
     * Builds a Venmo "pay" deep link that pre-fills the recipient, the total, and a note with
     * the request ID. On mobile this opens the Venmo app; on desktop it opens Venmo on the web.
     */
    private String buildVenmoPaymentLink(RentalRequestResponse request) {
        String note = URLEncoder.encode("Maddie West Events rental " + request.getId(), StandardCharsets.UTF_8);
        String amount = request.getTotalPrice().setScale(2, RoundingMode.HALF_UP).toPlainString();
        return "https://venmo.com/?txn=pay"
                + "&recipients=" + venmoUsername
                + "&amount=" + amount
                + "&note=" + note;
    }

    /** Builds a deep link to this request in the admin dashboard so the coordinator can review it. */
    private String buildAdminRequestUrl(RentalRequestResponse request) {
        String base = adminBaseUrl.endsWith("/") ? adminBaseUrl.substring(0, adminBaseUrl.length() - 1) : adminBaseUrl;
        return base + "/inquiries/" + request.getId();
    }

    @Async
    public void sendRejectionNotification(RentalRequestResponse request, String reason) {
        Context context = new Context();
        context.setVariable("request", request);
        context.setVariable("reason", reason);
        sendHtmlEmail(request.getRequester().getEmail(), coordinatorEmail, "Update on your rental request",
                "email/request-rejected-customer", context);
    }

    @Async
    public void sendCancellationNotification(RentalRequestResponse request, String reason) {
        Context context = new Context();
        context.setVariable("request", request);
        context.setVariable("reason", reason);
        sendHtmlEmail(request.getRequester().getEmail(), coordinatorEmail, "Your rental has been cancelled",
                "email/request-cancelled-customer", context);
    }

    @Async
    public void sendContactFormNotification(ContactFormRequest contact) {
        Context context = new Context();
        context.setVariable("contact", contact);
        // Reply-To the submitter so the coordinator can respond to them directly.
        sendHtmlEmail(coordinatorEmail, contact.getEmail(),
                "New contact form submission from " + contact.getName(),
                "email/contact-form-coordinator", context);
    }

    private void sendHtmlEmail(String to, String replyTo, String subject, String template, Context context) {
        try {
            String html = templateEngine.process(template, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send '{}' email to {}", subject, to, ex);
        }
    }
}
