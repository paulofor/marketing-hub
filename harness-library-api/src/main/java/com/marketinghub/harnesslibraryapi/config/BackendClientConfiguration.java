package com.marketinghub.harnesslibraryapi.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Configura o cliente HTTP interno com limites explícitos de conexão e leitura. */
@Configuration
public class BackendClientConfiguration {

  /**
   * Cria um cliente sem retry automático para que mutações dependam apenas de idempotência
   * explícita.
   */
  @Bean
  public RestClient harnessBackendRestClient(HarnessLibraryProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(
        defaultDuration(properties.connectTimeout(), Duration.ofSeconds(5)));
    requestFactory.setReadTimeout(
        defaultDuration(properties.readTimeout(), Duration.ofSeconds(15)));
    return RestClient.builder()
        .baseUrl(properties.backendBaseUrl())
        .requestFactory(requestFactory)
        .build();
  }

  /** Substitui somente duração ausente por um limite seguro. */
  private Duration defaultDuration(Duration configured, Duration fallback) {
    return configured == null ? fallback : configured;
  }
}
