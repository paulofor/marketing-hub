package com.marketinghub.zipper.service;

import com.marketinghub.zipper.config.StorageProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public StorageService(S3Client s3Client, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    public byte[] download(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(key)
                    .build();
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return response.asByteArray();
        } catch (SdkException ex) {
            log.error("Falha ao baixar arquivo '{}' do bucket '{}'", key, storageProperties.getBucket(), ex);
            throw new IllegalStateException("Não foi possível baixar arquivo do bucket", ex);
        }
    }

    public void upload(String key, byte[] bytes, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            log.info("Arquivo '{}' salvo em {}", key, storageProperties.getBucket());
        } catch (SdkException ex) {
            log.error("Falha ao salvar arquivo '{}' no bucket '{}'", key, storageProperties.getBucket(), ex);
            throw new IllegalStateException("Não foi possível salvar ZIP no bucket", ex);
        }
    }

    public Resource downloadAsResource(String key) {
        byte[] bytes = download(key);
        return new ByteArrayResource(bytes);
    }

    public Optional<String> buildPublicUrl(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String base = storageProperties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            return Optional.empty();
        }
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
        return Optional.of(normalized + "/" + encodedKey);
    }
}
