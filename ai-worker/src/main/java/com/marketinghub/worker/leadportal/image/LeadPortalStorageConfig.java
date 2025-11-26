package com.marketinghub.worker.leadportal.image;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class LeadPortalStorageConfig {

    @Bean
    public S3Client leadPortalS3Client(LeadPortalStorageProperties properties) {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey());

        S3Configuration configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        S3ClientBuilder builder =
                S3Client.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(credentials))
                        .region(Region.of(properties.getRegion()))
                        .serviceConfiguration(configuration);

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder = builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }
}
