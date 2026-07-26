package com.maddiewest.events.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Cloudflare R2 (S3-compatible) configuration. Values are supplied via environment
 * variables / the local .env file (R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, ...).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.r2")
public class R2Properties {

    /** Cloudflare account id, used to build the S3 API endpoint. */
    private String accountId;

    /** R2 API token access key id. */
    private String accessKeyId;

    /** R2 API token secret access key. */
    private String secretAccessKey;

    /** Target bucket name (e.g. maddie-west-rentals-dev). */
    private String bucket;

    /** Public base URL for reading objects (pub-*.r2.dev or a custom domain), no trailing slash. */
    private String publicBaseUrl;

    /** True once all required values are present, so uploads can proceed. */
    public boolean isConfigured() {
        return StringUtils.hasText(accountId)
                && StringUtils.hasText(accessKeyId)
                && StringUtils.hasText(secretAccessKey)
                && StringUtils.hasText(bucket)
                && StringUtils.hasText(publicBaseUrl);
    }

    /** S3 API endpoint for this account. */
    public String endpoint() {
        return "https://" + accountId + ".r2.cloudflarestorage.com";
    }
}
