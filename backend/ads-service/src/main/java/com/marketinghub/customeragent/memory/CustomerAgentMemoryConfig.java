package com.marketinghub.customeragent.memory;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/** Responsabilidade: configurar acesso AWS S3 exclusivo da memoria do Agente Cliente. */
@Configuration
public class CustomerAgentMemoryConfig {
  /** Cria cliente S3 usando IAM/credenciais do ambiente, sem segredo versionado. */
  @Bean("customerAgentMemoryS3Client")
  S3Client customerAgentMemoryS3Client(CustomerAgentMemoryProperties properties) {
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
