package com.marketinghub.storage;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public S3Client leadPortalS3Client(StorageProperties properties) {
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .serviceConfiguration(s3Configuration);

        if (isBlank(properties.getAccessKeyId()) || isBlank(properties.getSecretAccessKey())) {
            builder = builder.credentialsProvider(AnonymousCredentialsProvider.create());
        } else {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    properties.getAccessKeyId(),
                    properties.getSecretAccessKey());
            builder = builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder = builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
