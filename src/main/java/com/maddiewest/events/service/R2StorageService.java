package com.maddiewest.events.service;

import com.maddiewest.events.config.R2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Stores rental item images in a Cloudflare R2 bucket and returns their public URLs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class R2StorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif");

    /** Object key prefix so item images stay grouped within the bucket. */
    private static final String KEY_PREFIX = "items/";

    private final S3Client r2Client;
    private final R2Properties properties;

    /**
     * Uploads a single image and returns the public URL it can be served from.
     */
    public String upload(MultipartFile file) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "Image storage is not configured. Set the R2_* values in the rental-service .env file.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No image file was provided.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported image type. Allowed types: JPEG, PNG, WebP, GIF, AVIF.");
        }

        String key = KEY_PREFIX + UUID.randomUUID() + extensionFor(file.getOriginalFilename(), contentType);

        try {
            r2Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(key)
                            .contentType(contentType)
                            .cacheControl("public, max-age=31536000, immutable")
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read the uploaded image.", e);
        }

        String url = properties.getPublicBaseUrl().replaceAll("/+$", "") + "/" + key;
        log.info("Uploaded rental image to R2: {}", key);
        return url;
    }

    /**
     * Deletes an image previously returned by {@link #upload(MultipartFile)}. No-op if the URL does
     * not belong to the configured public base URL.
     */
    public void deleteByUrl(String url) {
        if (!properties.isConfigured() || !StringUtils.hasText(url)) {
            return;
        }
        String base = properties.getPublicBaseUrl().replaceAll("/+$", "") + "/";
        if (!url.startsWith(base)) {
            log.warn("Skipping delete for image URL outside the configured bucket: {}", url);
            return;
        }
        String key = url.substring(base.length());
        r2Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build());
        log.info("Deleted rental image from R2: {}", key);
    }

    private String extensionFor(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
            if (ext.matches("\\.[a-z0-9]{1,5}")) {
                return ext;
            }
        }
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/avif" -> ".avif";
            default -> "";
        };
    }
}
