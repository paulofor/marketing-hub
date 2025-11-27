package com.marketinghub.worker.leadportal.image;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class LeadPortalStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalStorageConfig.class);

    @Bean
    public S3Client leadPortalS3Client(LeadPortalStorageProperties properties) {
        S3Configuration configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        S3ClientBuilder builder = S3Client.builder().serviceConfiguration(configuration);

        if (StringUtils.hasText(properties.getRegion())) {
            builder = builder.region(Region.of(properties.getRegion().trim()));
        }

        if (StringUtils.hasText(properties.getAccessKeyId()) && StringUtils.hasText(properties.getSecretAccessKey())) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    properties.getAccessKeyId().trim(), properties.getSecretAccessKey().trim());
            builder = builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            log.warn("lead-portal storage credentials not configured; relying on default AWS credentials provider chain");
        }

        if (StringUtils.hasText(properties.getEndpoint())) {
            builder = builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
        }

        return builder.build();
    }
}
