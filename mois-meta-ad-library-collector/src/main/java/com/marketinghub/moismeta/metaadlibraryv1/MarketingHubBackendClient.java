package com.marketinghub.moismeta.metaadlibraryv1;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Consome somente os contratos internos MOIS Meta do backend principal. */
@Component
@Slf4j
public class MarketingHubBackendClient {

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final ObjectMapper objectMapper;
  private final String backendBaseUrl;

  /** Configura a conexão do coletor com o backend. */
  public MarketingHubBackendClient(
      ObjectMapper objectMapper, @Value("${marketinghub.backend-base-url}") String backendBaseUrl) {
    this.objectMapper = objectMapper;
    this.backendBaseUrl = backendBaseUrl;
  }

  /** Reserva uma investigação pendente, tratando 204 como fila vazia. */
  public Optional<MetaAdLibraryContracts.PendingInvestigation> pending() {
    HttpResponse<String> response =
        send("GET", "/api/internal/mois/meta-ad-library/v1/investigations/pending", null, null);
    if (response.statusCode() == 204) return Optional.empty();
    ensureSuccess(response, "buscar pendência");
    try {
      return Optional.of(
          objectMapper.readValue(response.body(), MetaAdLibraryContracts.PendingInvestigation.class));
    } catch (Exception ex) {
      log.error("Falha ao interpretar pendência MOIS Meta payload={}", response.body(), ex);
      throw new IllegalStateException("Contrato de pendência Meta inválido", ex);
    }
  }

  /** Envia as observações reais ao backend. */
  public void observations(long investigationId, MetaAdLibraryContracts.ObservationBatch batch) {
    HttpResponse<String> response =
        send(
            "POST",
            "/api/internal/mois/meta-ad-library/v1/investigations/"
                + investigationId
                + "/observations",
            batch,
            investigationId);
    ensureSuccess(response, "enviar observações");
  }

  /** Reporta sucesso ou falha técnica da execução. */
  public void complete(long investigationId, boolean success, String errorMessage) {
    HttpResponse<String> response =
        send(
            "POST",
            "/api/internal/mois/meta-ad-library/v1/investigations/"
                + investigationId
                + "/complete",
            new MetaAdLibraryContracts.Completion(success, errorMessage),
            investigationId);
    ensureSuccess(response, "concluir investigação");
  }

  /** Executa uma chamada HTTP auditável ao backend. */
  private HttpResponse<String> send(String method, String path, Object body, Long investigationId) {
    String url = backendBaseUrl + path;
    try {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30));
      if (body == null) builder.GET();
      else
        builder
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
      log.info("MOIS Meta backend request investigationId={} method={} url={}", investigationId, method, url);
      HttpResponse<String> response =
          httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      log.info(
          "MOIS Meta backend response investigationId={} status={} url={} payload={}",
          investigationId,
          response.statusCode(),
          url,
          response.body());
      return response;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Chamada interrompida ao backend investigationId={} url={}", investigationId, url, ex);
      throw new IllegalStateException("Chamada ao backend interrompida", ex);
    } catch (Exception ex) {
      log.error("Falha ao chamar backend investigationId={} url={}", investigationId, url, ex);
      throw new IllegalStateException("Falha ao chamar backend", ex);
    }
  }

  /** Bloqueia respostas HTTP de erro com contexto operacional. */
  private void ensureSuccess(HttpResponse<String> response, String operation) {
    if (response.statusCode() / 100 != 2) {
      throw new IllegalStateException(
          "Falha ao " + operation + ": HTTP " + response.statusCode() + " " + response.body());
    }
  }
}
