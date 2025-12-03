package com.marketinghub.watermark.service;

import com.marketinghub.watermark.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            throw new IllegalStateException("Não foi possível baixar a imagem original para aplicar marca d'água", ex);
        }
    }

    public void upload(String key, byte[] data, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(data));
            log.info("Imagem com marca d'água armazenada em {}/{}", storageProperties.getBucket(), key);
        } catch (SdkException ex) {
            log.error("Falha ao armazenar arquivo '{}' no bucket '{}'", key, storageProperties.getBucket(), ex);
            throw new IllegalStateException("Não foi possível salvar a imagem com marca d'água", ex);
        }
    }
}
