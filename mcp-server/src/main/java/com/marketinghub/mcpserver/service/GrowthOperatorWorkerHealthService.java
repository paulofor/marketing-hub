package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Consulta o health do Operador de Crescimento no host real do executor. */
@Service
public class GrowthOperatorWorkerHealthService {
  private static final Logger logger =
      LoggerFactory.getLogger(GrowthOperatorWorkerHealthService.class);
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final String healthUrl;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  /** Inicializa a consulta com URL restrita e timeout de rede. */
  public GrowthOperatorWorkerHealthService(
      @Value("${mcp.growth-operator-worker-health-url}") String healthUrl,
      ObjectMapper objectMapper) {
    this.healthUrl = healthUrl;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  /** Lê e estrutura o health HTTP publicado pelo worker. */
  public Map<String, Object> readHealth() {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(healthUrl))
              .timeout(Duration.ofSeconds(10))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalArgumentException(
            "growth operator worker health failed: HTTP " + response.statusCode());
      }
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("module", "growth-operator-worker");
      result.put("healthUrl", healthUrl);
      result.put("reachable", true);
      result.put("payload", objectMapper.readValue(response.body(), MAP_TYPE));
      return result;
    } catch (IOException ex) {
      logger.error("Falha ao ler health do growth-operator-worker; url={}", healthUrl, ex);
      throw new IllegalArgumentException(
          "failed to read growth operator worker health: " + ex.getMessage());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      logger.error("Leitura de health do growth-operator-worker interrompida; url={}", healthUrl, ex);
      throw new IllegalArgumentException("growth operator worker health interrupted");
    }
  }
}
