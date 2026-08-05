package com.marketinghub.experimentstrategist.memory;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/** Responsabilidade: configurar o acesso privado ao S3 da memoria do Estrategista. */
@Configuration
public class ExperimentStrategistMemoryConfig {
  /** Cria o cliente S3 com credenciais fornecidas pelo ambiente. */
  @Bean("experimentStrategistMemoryS3Client")
  S3Client experimentStrategistMemoryS3Client(ExperimentStrategistMemoryProperties properties) {
    S3ClientBuilder builder =
        S3Client.builder()
            .region(Region.of(properties.getRegion()))
            .credentialsProvider(DefaultCredentialsProvider.create());
    if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
      builder.endpointOverride(URI.create(properties.getEndpoint()));
    }
    return builder.build();
  }
}
