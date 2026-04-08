package com.marketinghub.worker.frameworkimage;

import com.marketinghub.worker.leadportal.image.LeadPortalStorageProperties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class FrameworkImageStorageClient {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageStorageClient.class);
    private static final String DEFAULT_PREFIX = "framework-image";

    private final LeadPortalStorageProperties properties;
    private final S3Client s3Client;

    public FrameworkImageStorageClient(LeadPortalStorageProperties properties,
                                       @Qualifier("leadPortalS3Client") S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    public UploadedFrameworkImage upload(byte[] content, String preferredName) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Framework image content must not be empty");
        }
        if (!StringUtils.hasText(properties.getBucket())) {
            throw new IllegalStateException("lead-portal.storage.bucket must be configured");
        }

        String key = buildObjectKey(preferredName);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(MediaType.IMAGE_JPEG_VALUE)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        String publicUrl = resolvePublicUrl(key);
        log.info("Framework image uploaded to Cloudflare key={} bytes={}", key, content.length);
        return new UploadedFrameworkImage(key, publicUrl);
    }

    private String buildObjectKey(String preferredName) {
        String prefix = DEFAULT_PREFIX;
        String baseName = StringUtils.hasText(preferredName) ? sanitize(preferredName) : UUID.randomUUID().toString();
        if (!baseName.endsWith(".jpg") && !baseName.endsWith(".jpeg")) {
            baseName = baseName + ".jpg";
        }
        if (prefix.endsWith("/")) {
            return prefix + baseName;
        }
        return prefix + "/" + baseName;
    }

    private String sanitize(String value) {
        return value.toLowerCase()
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-{2,}", "-");
    }

    private String resolvePublicUrl(String key) {
        if (!StringUtils.hasText(properties.getPublicBaseUrl())) {
            return key;
        }
        String base = properties.getPublicBaseUrl().endsWith("/")
                ? properties.getPublicBaseUrl().substring(0, properties.getPublicBaseUrl().length() - 1)
                : properties.getPublicBaseUrl();
        return base + "/" + key;
    }

    public record UploadedFrameworkImage(String objectKey, String publicUrl) {}
}
