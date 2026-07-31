package com.marketinghub.experiment.monitoring.pde;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Consulta o backend PDE por HTTP para obter métricas administrativas do funil versionado. */
@Component
public class PdeAnalyticsHttpClient implements PdeAnalyticsClient {

  private static final String DEFAULT_PDE_BASE_URL = "https://v5.clubemusa.com.br";

  private final RestClient restClient;
  private final JdkClientHttpRequestFactory requestFactory;

  /** Inicializa o cliente HTTP com timeouts configuráveis para não zerar o cockpit por lentidão. */
  public PdeAnalyticsHttpClient(
      @Value("${integrations.pde-platform.base-url:" + DEFAULT_PDE_BASE_URL + "}") String baseUrl,
      @Value("${integrations.pde-platform.connect-timeout:PT3S}") Duration connectTimeout,
      @Value("${integrations.pde-platform.read-timeout:PT15S}") Duration readTimeout) {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    this.requestFactory = new JdkClientHttpRequestFactory(httpClient);
    this.requestFactory.setReadTimeout(readTimeout);
    this.restClient =
        RestClient.builder()
            .baseUrl(trimTrailingSlash(baseUrl))
            .requestFactory(this.requestFactory)
            .build();
  }

  /** Busca o resumo consolidado de analytics do produto no backend PDE. */
  @Override
  public PdeAnalyticsSummary fetchSummary(String productSlug) {
    return restClient
        .get()
        .uri("/api/pde/access/analytics/{productSlug}/summary", productSlug)
        .retrieve()
        .body(PdeAnalyticsSummary.class);
  }

  /** Busca o resumo consolidado usando explicitamente a URL pública da versão PDE do experimento. */
  @Override
  public PdeAnalyticsSummary fetchSummary(String productSlug, String publicBaseUrl) {
    if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
      return fetchSummary(productSlug);
    }
    return RestClient.builder()
        .baseUrl(trimTrailingSlash(publicBaseUrl))
        .requestFactory(requestFactory)
        .build()
        .get()
        .uri("/api/pde/access/analytics/{productSlug}/summary", productSlug)
        .retrieve()
        .body(PdeAnalyticsSummary.class);
  }

  /** Remove barra final para montar rotas internas de forma previsível. */
  private String trimTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      return DEFAULT_PDE_BASE_URL;
    }
    return value.replaceAll("/+$", "");
  }
}
