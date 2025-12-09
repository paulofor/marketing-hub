package com.marketinghub.storage;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Minimal storage service used by the ads-service to fetch lead portal assets hosted on Cloudflare R2.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final StorageProperties properties;
    private final S3Client s3Client;

    public FileStorageService(StorageProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (isBlank(properties.getBucket())
                || isBlank(properties.getEndpoint())
                || isBlank(properties.getAccessKeyId())
                || isBlank(properties.getSecretAccessKey())) {
            log.warn("Lead portal storage is not fully configured; e-mails with anexos may fail");
        }
    }

    public Resource loadAsResource(String storedFileName) {
        if (isBlank(storedFileName)) {
            throw new StorageException("storedFileName must be provided");
        }
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storedFileName)
                    .build();
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return new ByteArrayResource(response.asByteArray());
        } catch (SdkException ex) {
            throw new StorageException("Failed to load file from bucket", ex);
        }
    }

    public Optional<String> resolvePublicUrl(String storedFileName) {
        if (isBlank(storedFileName)) {
            return Optional.empty();
        }
        String base = properties.getPublicBaseUrl();
        if (isBlank(base)) {
            return Optional.empty();
        }
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return Optional.of(normalized + "/" + storedFileName);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
