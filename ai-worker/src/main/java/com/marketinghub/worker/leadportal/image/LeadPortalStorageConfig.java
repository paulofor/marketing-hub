package com.marketinghub.worker.leadportal.image;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Responsabilidade: configurar o cliente S3 compartilhado pelo AI Worker para arquivos do Lead Portal.
 */
@Configuration
public class LeadPortalStorageConfig {

    private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration API_CALL_ATTEMPT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Cria o cliente R2 com timeouts para impedir que downloads/uploads de imagem prendam o ciclo do worker.
     */
    @Bean
    @ConditionalOnMissingBean(name = "leadPortalS3Client")
    public S3Client leadPortalS3Client(LeadPortalStorageProperties properties) {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey());

        S3Configuration configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        ClientOverrideConfiguration timeoutConfiguration = ClientOverrideConfiguration.builder()
                .apiCallTimeout(API_CALL_TIMEOUT)
                .apiCallAttemptTimeout(API_CALL_ATTEMPT_TIMEOUT)
                .build();

        S3ClientBuilder builder =
                S3Client.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(credentials))
                        .region(Region.of(properties.getRegion()))
                        .serviceConfiguration(configuration)
                        .overrideConfiguration(timeoutConfiguration);

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder = builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }
}
