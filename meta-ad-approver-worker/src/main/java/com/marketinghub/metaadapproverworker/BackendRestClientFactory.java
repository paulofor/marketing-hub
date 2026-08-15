package com.marketinghub.metaadapproverworker;

import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Responsabilidade: criar clientes do backend com limites operacionais contra callbacks travados.
 */
final class BackendRestClientFactory {
  private BackendRestClientFactory() {}

  /** Cria o cliente compartilhando os timeouts canônicos do executor de Têmis. */
  static RestClient create(MetaAdApproverProperties properties) {
    HttpClient httpClient =
        HttpClient.newBuilder().connectTimeout(properties.getBackendConnectTimeout()).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.getBackendReadTimeout());
    return RestClient.builder()
        .baseUrl(properties.getBackendUrl())
        .requestFactory(requestFactory)
        .build();
  }
}
