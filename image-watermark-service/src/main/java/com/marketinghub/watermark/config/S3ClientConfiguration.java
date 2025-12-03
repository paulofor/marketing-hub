package com.marketinghub.watermark.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3ClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(S3ClientConfiguration.class);

    private final StorageProperties storageProperties;

    public S3ClientConfiguration(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    void validateProperties() {
        if (isBlank(storageProperties.getBucket())
                || isBlank(storageProperties.getEndpoint())
                || isBlank(storageProperties.getAccessKeyId())
                || isBlank(storageProperties.getSecretAccessKey())) {
            throw new IllegalStateException("Configurações de armazenamento inválidas para o serviço de marca d'água");
        }
    }

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                storageProperties.getAccessKeyId(), storageProperties.getSecretAccessKey());

        software.amazon.awssdk.services.s3.S3Configuration serviceConfiguration =
                software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build();

        Region region = Region.of(storageProperties.getRegion());

        log.info("Inicializando cliente S3 para marca d'água (bucket: {})", storageProperties.getBucket());
        return S3Client.builder()
                .endpointOverride(URI.create(storageProperties.getEndpoint()))
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(serviceConfiguration)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
