package com.marketinghub.metaadapproverworker;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Responsabilidade: criar clientes do backend com limites operacionais contra callbacks travados.
 */
final class BackendRestClientFactory {
  private BackendRestClientFactory() {}

  /** Cria o cliente compartilhando os timeouts canônicos do executor de Têmis. */
  static RestClient create(MetaAdApproverProperties properties) {
    return create(properties, properties.getBackendReadTimeout());
  }

  /** Cria um cliente com timeout de leitura específico para a operação chamadora. */
  static RestClient create(MetaAdApproverProperties properties, Duration readTimeout) {
    HttpClient httpClient =
        HttpClient.newBuilder().connectTimeout(properties.getBackendConnectTimeout()).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(readTimeout);
    return RestClient.builder()
        .baseUrl(properties.getBackendUrl())
        .requestFactory(requestFactory)
        .build();
  }
}
