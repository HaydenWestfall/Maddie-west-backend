package com.maddiewest.rentalservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * Builds the S3 client used to talk to Cloudflare R2. The client is always created so the
 * application starts without R2 credentials; requests only fail (with a clear message) when an
 * upload is actually attempted against an unconfigured bucket. See {@link R2Properties}.
 */
@Configuration
@RequiredArgsConstructor
public class R2Config {

    private final R2Properties properties;

    @Bean
    public S3Client r2Client() {
        // Fall back to placeholder credentials so the client always constructs and the app starts
        // even before R2 is configured. R2StorageService checks isConfigured() before any real call,
        // so these placeholders are never used against the network.
        String accessKey = StringUtils.hasText(properties.getAccessKeyId())
                ? properties.getAccessKeyId() : "unset";
        String secretKey = StringUtils.hasText(properties.getSecretAccessKey())
                ? properties.getSecretAccessKey() : "unset";
        return S3Client.builder()
                // R2 uses a fixed "auto" region.
                .region(Region.of("auto"))
                .endpointOverride(URI.create(properties.endpoint()))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
