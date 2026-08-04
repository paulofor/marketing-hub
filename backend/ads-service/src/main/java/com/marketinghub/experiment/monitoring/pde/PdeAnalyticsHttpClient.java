package com.marketinghub.experiment.monitoring.pde;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Consulta o backend PDE por HTTP para obter métricas administrativas do funil versionado. */
@Component
public class PdeAnalyticsHttpClient implements PdeAnalyticsClient {

  private static final String DEFAULT_PDE_BASE_URL = "https://v5.clubemusa.com.br";

  private final String defaultBaseUrl;
  private final RestClient restClient;
  private final JdkClientHttpRequestFactory requestFactory;

  /** Inicializa o cliente HTTP com timeouts configuráveis para não zerar o cockpit por lentidão. */
  public PdeAnalyticsHttpClient(
      @Value("${integrations.pde-platform.base-url:" + DEFAULT_PDE_BASE_URL + "}") String baseUrl,
      @Value("${integrations.pde-platform.connect-timeout:PT3S}") Duration connectTimeout,
      @Value("${integrations.pde-platform.read-timeout:PT15S}") Duration readTimeout) {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    this.defaultBaseUrl = trimTrailingSlash(baseUrl);
    this.requestFactory = new JdkClientHttpRequestFactory(httpClient);
    this.requestFactory.setReadTimeout(readTimeout);
    this.restClient =
        RestClient.builder()
            .baseUrl(this.defaultBaseUrl)
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

  /** Busca a identidade da build PDE pela mesma origem administrativa usada nas métricas. */
  @Override
  public PdeBuildIdentity fetchBuildIdentity(String publicBaseUrl) {
    String baseUrl = trimTrailingSlash(publicBaseUrl != null ? publicBaseUrl : defaultBaseUrl);
    RestClient monitoredClient = monitoredClient(baseUrl);
    try {
      return monitoredClient
          .get()
          .uri("/api/pde/build-identity")
          .retrieve()
          .body(PdeBuildIdentity.class);
    } catch (RestClientResponseException ex) {
      return fetchBuildIdentityFromDeployStatus(monitoredClient, ex);
    }
  }

  /** Reaproveita o manifesto de deploy quando a rota nova de identidade ainda não existe. */
  private PdeBuildIdentity fetchBuildIdentityFromDeployStatus(
      RestClient monitoredClient, RestClientResponseException originalException) {
    try {
      PdeDeployStatus status =
          monitoredClient
              .get()
              .uri("/api/pde/deploy/status")
              .retrieve()
              .body(PdeDeployStatus.class);
      if (status == null) {
        throw originalException;
      }
      if (status.buildIdentity() != null) {
        return status.buildIdentity();
      }
      return status.toBuildIdentity();
    } catch (RuntimeException fallbackException) {
      originalException.addSuppressed(fallbackException);
      throw originalException;
    }
  }

  /** Cria um client dedicado para a origem pública monitorada. */
  private RestClient monitoredClient(String baseUrl) {
    return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
  }

  /**
   * Busca o resumo consolidado usando explicitamente a URL pública da versão PDE do experimento.
   */
  @Override
  public PdeAnalyticsSummary fetchSummary(String productSlug, String publicBaseUrl) {
    return fetchSummary(productSlug, publicBaseUrl, null);
  }

  /** Consulta métricas da versão informada para impedir mistura entre slots do mesmo produto. */
  @Override
  public PdeAnalyticsSummary fetchSummary(
      String productSlug, String publicBaseUrl, String experienceVersion) {
    if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
      return restClient
          .get()
          .uri(
              uriBuilder -> {
                var builder = uriBuilder.path("/api/pde/access/analytics/{productSlug}/summary");
                if (experienceVersion != null && !experienceVersion.isBlank()) {
                  builder.queryParam("experienceVersion", experienceVersion.trim());
                }
                return builder.build(productSlug);
              })
          .retrieve()
          .body(PdeAnalyticsSummary.class);
    }
    return RestClient.builder()
        .baseUrl(trimTrailingSlash(publicBaseUrl))
        .requestFactory(requestFactory)
        .build()
        .get()
        .uri(
            uriBuilder -> {
              var builder = uriBuilder.path("/api/pde/access/analytics/{productSlug}/summary");
              if (experienceVersion != null && !experienceVersion.isBlank()) {
                builder.queryParam("experienceVersion", experienceVersion.trim());
              }
              return builder.build(productSlug);
            })
        .retrieve()
        .body(PdeAnalyticsSummary.class);
  }

  /** Busca o resumo incluindo tráfego automatizado para validações operacionais fake. */
  @Override
  public PdeAnalyticsSummary fetchSummaryIncludingNonHumanTraffic(
      String productSlug, String publicBaseUrl) {
    if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
      return restClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/pde/access/analytics/{productSlug}/summary")
                      .queryParam("includeNonHumanTraffic", true)
                      .build(productSlug))
          .retrieve()
          .body(PdeAnalyticsSummary.class);
    }
    return RestClient.builder()
        .baseUrl(trimTrailingSlash(publicBaseUrl))
        .requestFactory(requestFactory)
        .build()
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/pde/access/analytics/{productSlug}/summary")
                    .queryParam("includeNonHumanTraffic", true)
                    .build(productSlug))
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

  /** Representa o manifesto de deploy usado como fallback por versões antigas da PDE. */
  private record PdeDeployStatus(
      PdeBuildIdentity buildIdentity,
      String environment,
      String commitSha,
      String branch,
      String imageTag,
      String frontendUrl,
      String backendUrl,
      Instant deployedAt,
      List<PdeDeployServiceStatus> services) {

    /** Converte campos legados do manifesto em identidade auditável para o cockpit. */
    private PdeBuildIdentity toBuildIdentity() {
      return new PdeBuildIdentity(
          "pde-platform-backend",
          "pde-platform-backend",
          null,
          commitSha,
          branch,
          imageTag,
          backendImage(),
          environment,
          backendUrl,
          frontendUrl,
          null,
          deployedAt);
    }

    /** Localiza a imagem do backend no manifesto legado de serviços publicados. */
    private String backendImage() {
      if (services == null) {
        return null;
      }
      return services.stream()
          .filter(service -> "backend".equalsIgnoreCase(service.role()))
          .map(PdeDeployServiceStatus::image)
          .filter(image -> image != null && !image.isBlank())
          .findFirst()
          .orElse(null);
    }
  }

  /** Representa um serviço declarado no manifesto de deploy PDE legado. */
  private record PdeDeployServiceStatus(String image, String role) {}
}
