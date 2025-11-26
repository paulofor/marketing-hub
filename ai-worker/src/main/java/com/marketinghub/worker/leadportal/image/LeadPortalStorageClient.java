package com.marketinghub.worker.leadportal.image;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Component
public class LeadPortalStorageClient {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalStorageClient.class);

    private final LeadPortalStorageProperties properties;
    private final S3Client s3Client;

    public LeadPortalStorageClient(LeadPortalStorageProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    public byte[] download(String storedFileName) {
        if (!StringUtils.hasText(storedFileName)) {
            throw new IllegalArgumentException("Stored file name must not be empty");
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(storedFileName)
                .build();

        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return response.asByteArray();
        } catch (SdkException ex) {
            log.error("Failed to download '{}' from bucket '{}'", storedFileName, properties.getBucket(), ex);
            throw ex;
        }
    }

    public StoredImage upload(byte[] content, String preferredFilename, MediaType mediaType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Image content must not be empty");
        }
        String extension = mediaType != null && StringUtils.hasText(mediaType.getSubtype())
                ? mediaType.getSubtype()
                : "bin";
        String cleanedName = StringUtils.hasText(preferredFilename) ? preferredFilename.trim() : "image";
        String objectKey = UUID.randomUUID() + "-" + cleanedName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(mediaType != null ? mediaType.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .build();
        try {
            PutObjectResponse response = s3Client.putObject(request, RequestBody.fromBytes(content));
            log.info(
                    "Stored file '{}' ({} bytes, etag={}) in bucket '{}'",
                    objectKey,
                    content.length,
                    response.eTag(),
                    properties.getBucket());
        } catch (SdkException ex) {
            log.error("Failed to upload '{}' to bucket '{}'", objectKey, properties.getBucket(), ex);
            throw ex;
        }

        return new StoredImage(objectKey, resolvePublicUrl(objectKey), extension);
    }

    private String resolvePublicUrl(String objectKey) {
        return Optional.ofNullable(properties.getPublicBaseUrl())
                .filter(StringUtils::hasText)
                .map(base -> normalizeBase(base) + "/" + objectKey)
                .orElse(objectKey);
    }

    private String normalizeBase(String base) {
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }

    public record StoredImage(String objectKey, String publicUrl, String extension) {}
}
