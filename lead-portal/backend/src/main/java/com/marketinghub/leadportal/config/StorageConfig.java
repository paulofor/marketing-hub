package com.marketinghub.leadportal.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Responsabilidade: configurar o cliente S3 usado pelo Lead Portal para acessar o storage R2.
 */
@Configuration
public class StorageConfig {

    private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration API_CALL_ATTEMPT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Cria o cliente R2 com timeout explícito para evitar travamento no envio de fotos.
     */
    @Bean
    public S3Client r2S3Client(StorageProperties properties) {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey());

        S3Configuration s3Configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        ClientOverrideConfiguration timeoutConfiguration = ClientOverrideConfiguration.builder()
                .apiCallTimeout(API_CALL_TIMEOUT)
                .apiCallAttemptTimeout(API_CALL_ATTEMPT_TIMEOUT)
                .build();

        S3ClientBuilder builder =
                S3Client.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(credentials))
                        .region(Region.of(properties.getRegion()))
                        .serviceConfiguration(s3Configuration)
                        .overrideConfiguration(timeoutConfiguration);

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }
}
