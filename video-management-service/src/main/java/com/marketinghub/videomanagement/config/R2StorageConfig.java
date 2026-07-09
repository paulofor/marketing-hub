package com.marketinghub.videomanagement.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Configura o cliente S3 compatível com Cloudflare R2 usado pelo módulo de vídeo.
 */
@Configuration
public class R2StorageConfig {

    /** Cria o client de storage usando apenas configuração externa do ambiente. */
    @Bean
    public S3Client videoR2S3Client(VideoManagementProperties properties) {
        VideoManagementProperties.Storage storage = properties.getStorage();
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
        var builder = S3Client.builder()
                .serviceConfiguration(s3Configuration)
                .region(Region.of(StringUtils.hasText(storage.getRegion()) ? storage.getRegion() : "auto"));
        URI endpoint = storage.getEndpoint();
        if (endpoint != null && StringUtils.hasText(endpoint.toString())) {
            builder.endpointOverride(endpoint);
        }
        if (StringUtils.hasText(storage.getAccessKeyId()) && StringUtils.hasText(storage.getSecretAccessKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(storage.getAccessKeyId(), storage.getSecretAccessKey())));
        }
        return builder.build();
    }
}
