package com.marketinghub.pde.vega.privatevalidation.v1;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: transportar contratos privados de Vega sem duplicar estado ou autoridade. */
@Service
public class VegaPrivateGatewayService {
  private static final Logger log = LoggerFactory.getLogger(VegaPrivateGatewayService.class);
  private final URI backend;
  private final HttpClient client;

  /** Reutiliza a integração canônica do catálogo sem injetar credenciais do servidor. */
  public VegaPrivateGatewayService(
      @Value("${pde.catalog.marketing-hub-base-url:}") String baseUrls) {
    this.backend =
        Arrays.stream(baseUrls.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(URI::create)
            .findFirst()
            .orElse(null);
    if (backend != null
        && (!Arrays.asList("http", "https").contains(backend.getScheme())
            || backend.getHost() == null
            || backend.getUserInfo() != null
            || backend.getQuery() != null
            || backend.getFragment() != null))
      throw new IllegalArgumentException(
          "Integração privada PDE exige uma URL de backend sem segredo.");
    this.client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  /** Encaminha somente operações tipadas, mantendo status e a credencial recebida do chamador. */
  public ResponseEntity<String> forward(
      String method, String path, String body, String header, String token) {
    if (backend == null)
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Integração privada de Vega ainda não está configurada no backend PDE.");
    if (!path.matches(
        "/(contract|access|session|start|generate|events|finish|internal/sessions(?:/[a-f0-9-]{36})?|internal/cycles/[1-9][0-9]*/report|internal/adjustment/stage-executions/(pending\\?mode=(FIXTURE|PROVIDER)|[1-9][0-9]*/(claim|request|result)))"))
      throw new IllegalArgumentException("Operação fora do contrato privado de Vega.");
    URI endpoint = backend.resolve("/api/pde/vega/private/v1" + path);
    var builder =
        HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");
    if (token != null && !token.isBlank()) builder.header(header, token);
    builder.method(
        method,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body));
    try {
      var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 500)
        log.warn(
            "Falha remota no contrato privado PDE operation={} endpoint={} status={}",
            method,
            endpoint,
            response.statusCode());
      return ResponseEntity.status(response.statusCode())
          .header("Content-Type", "application/json")
          .header("Cache-Control", "private, no-store")
          .header("Referrer-Policy", "no-referrer")
          .body(response.body());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error(
          "Transporte privado PDE interrompido operation={} endpoint={}", method, endpoint, ex);
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "A operação foi interrompida; consulte o estado preservado.", ex);
    } catch (IOException ex) {
      log.error("Falha no transporte privado PDE operation={} endpoint={}", method, endpoint, ex);
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "Não foi possível consultar Vega agora; o estado permanece no backend.",
          ex);
    }
  }
}
