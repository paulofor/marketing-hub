package com.marketinghub.leadportal.storage;

import com.marketinghub.leadportal.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Optional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class FileStorageService {

    private final StorageProperties properties;
    private final S3Client s3Client;

    public FileStorageService(StorageProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void init() {
        if (isBlank(properties.getBucket())
                || isBlank(properties.getEndpoint())
                || isBlank(properties.getAccessKeyId())
                || isBlank(properties.getSecretAccessKey())) {
            throw new StorageException("Storage is not fully configured for Cloudflare R2");
        }
    }

    public String store(MultipartFile file, String identifier) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.isEmpty()) {
            filename = "upload";
        }
        String storedFileName = identifier + "-" + filename;
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storedFileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return storedFileName;
        } catch (SdkException | IOException ex) {
            throw new StorageException("Failed to store file", ex);
        }
    }

    public Resource loadAsResource(String storedFileName) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storedFileName)
                    .build();
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
            return new ByteArrayResource(objectBytes.asByteArray());
        } catch (SdkException ex) {
            throw new StorageFileNotFoundException("File not found: " + storedFileName, ex);
        }
    }

    public Optional<String> resolvePublicUrl(String storedFileName) {
        if (isBlank(storedFileName)) {
            return Optional.empty();
        }

        String publicBaseUrl = properties.getPublicBaseUrl();
        if (isBlank(publicBaseUrl)) {
            return Optional.empty();
        }

        String normalizedBase = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return Optional.of(normalizedBase + "/" + storedFileName);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
