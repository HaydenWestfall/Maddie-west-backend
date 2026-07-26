package com.maddiewest.rentalservice.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Access log for every incoming request. Runs before Spring Security (and everything else)
 * so the correlation id is set in the MDC for the whole request, including auth failures and
 * exception handling. Logs the inbound request at DEBUG and the outcome (status + duration)
 * at INFO/WARN/ERROR depending on the response status.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter implements Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String ip = clientIp(request);

        long start = System.currentTimeMillis();
        log.debug("--> {} {}{} [{}]", method, uri, query != null ? "?" + query : "", ip);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            if (status >= 500) {
                log.error("<-- {} {} {} ({}ms) [{}]", method, uri, status, durationMs, ip);
            } else if (status >= 400) {
                log.warn("<-- {} {} {} ({}ms) [{}]", method, uri, status, durationMs, ip);
            } else {
                log.info("<-- {} {} {} ({}ms) [{}]", method, uri, status, durationMs, ip);
            }
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        return StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
