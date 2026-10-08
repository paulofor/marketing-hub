package com.marketinghub.pde.visualpersonalization.v1;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Responsabilidade: transportar contratos visuais PDE sem duplicar estado, autorização ou execução. */
@Service
public class VisualPreparationGatewayService {
  private static final Logger log = LoggerFactory.getLogger(VisualPreparationGatewayService.class);
  private final URI backend;
  private final HttpClient client;

  /** Reutiliza a origem principal configurada no catálogo PDE, sem credenciais na URL. */
  public VisualPreparationGatewayService(@Value("${pde.catalog.marketing-hub-base-url:}") String baseUrls) {
    this.backend = Arrays.stream(baseUrls.split(",")).map(String::trim).filter(s -> !s.isEmpty())
        .map(URI::create).findFirst().orElse(null);
    if (backend != null && (!Arrays.asList("http", "https").contains(backend.getScheme())
        || backend.getHost() == null || backend.getUserInfo() != null
        || backend.getQuery() != null || backend.getFragment() != null))
      throw new IllegalArgumentException("A integração PDE exige origem principal sem segredo.");
    this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER).build();
  }

  /** Encaminha exclusivamente consultas privadas, pending e callbacks tipados da mesma etapa. */
  public ResponseEntity<String> forward(String method, String suffix, String body, String header, String token) {
    if (backend == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Origem principal PDE ausente.");
    if (!suffix.matches("/(preparations/pde-visual-v1-[a-f0-9-]{36}(/reconcile)?|internal/stage-executions/(pending|pde-visual-v1-[a-f0-9-]{36}/(claim|request|result|replay)))"))
      throw new IllegalArgumentException("Operação fora do contrato visual PDE.");
    URI endpoint = backend.resolve("/api/pde/visual-personalization/v1" + suffix);
    var builder = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(30))
        .header("Accept", "application/json").header("Content-Type", "application/json");
    if (token != null && !token.isBlank()) builder.header(header, token);
    builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
    try {
      var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      return ResponseEntity.status(response.statusCode()).header("Content-Type", "application/json")
          .header("Cache-Control", "private, no-store").header("Referrer-Policy", "no-referrer").body(response.body());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Transporte visual PDE interrompido operation={} endpoint={}", method, endpoint, ex);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Consulte o estado preservado da preparação.", ex);
    } catch (IOException ex) {
      log.error("Falha no transporte visual PDE operation={} endpoint={}", method, endpoint, ex);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível consultar a preparação agora.", ex);
    }
  }
}
