package com.marketinghub.landinggeneratoragent;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Responsabilidade: criar clientes HTTP do backend com limites de conexão e leitura. */
final class LandingGeneratorBackendRestClientFactory {
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

  /** Impede instanciação porque a fábrica não mantém estado. */
  private LandingGeneratorBackendRestClientFactory() {}

  /** Cria o cliente produtivo com prazo menor que o intervalo do polling de Dédalo. */
  static RestClient create(String backendUrl) {
    return create(backendUrl, CONNECT_TIMEOUT, READ_TIMEOUT);
  }

  /** Aplica os mesmos limites ao builder administrado pelo Spring. */
  static RestClient.Builder configure(RestClient.Builder builder) {
    return configure(builder, CONNECT_TIMEOUT, READ_TIMEOUT);
  }

  /** Cria um cliente com limites explícitos para testes determinísticos de indisponibilidade. */
  static RestClient create(String backendUrl, Duration connectTimeout, Duration readTimeout) {
    return configure(RestClient.builder().baseUrl(backendUrl), connectTimeout, readTimeout).build();
  }

  /** Configura a fábrica de requests sem alterar headers ou URL definidos pelo consumidor. */
  private static RestClient.Builder configure(
      RestClient.Builder builder, Duration connectTimeout, Duration readTimeout) {
    SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
    requests.setConnectTimeout(connectTimeout);
    requests.setReadTimeout(readTimeout);
    return builder.requestFactory(requests);
  }
}
