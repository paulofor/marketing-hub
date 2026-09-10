package com.marketinghub.agentmonitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * Responsabilidade: resolver a versão técnica exigida de cada executor pelo manifesto publicado.
 */
final class AgentExecutorVersionCatalog {
  private static final Logger log = LoggerFactory.getLogger(AgentExecutorVersionCatalog.class);
  private static final String RESOURCE =
      "agent-runtime/config/agents/codex-agent-health-compliance.json";
  private final Map<String, Integer> versions;

  /** Valida o manifesto sem confundir versões editáveis de cadastro com versões de executores. */
  AgentExecutorVersionCatalog(JsonNode document) {
    if (!"codex-health-v1".equals(document.path("contractVersion").asText())
        || !document.path("agents").isArray()
        || document.path("agents").isEmpty()) {
      throw new IllegalStateException("Manifesto de versões dos executores inválido.");
    }
    Map<String, Integer> indexed = new HashMap<>();
    for (JsonNode agent : document.path("agents")) {
      String key = agent.path("key").asText();
      JsonNode version = agent.path("expectedVersion");
      if (key.isBlank()
          || !version.isIntegralNumber()
          || !version.canConvertToInt()
          || version.asInt() < 1
          || indexed.putIfAbsent(key, version.asInt()) != null) {
        throw new IllegalStateException("Versão técnica ausente, inválida ou duplicada: " + key);
      }
    }
    versions = Map.copyOf(indexed);
  }

  /** Carrega a fonte única empacotada no JAR e bloqueia a inicialização se estiver inválida. */
  static AgentExecutorVersionCatalog load() {
    try (var input = new ClassPathResource(RESOURCE).getInputStream()) {
      return new AgentExecutorVersionCatalog(new ObjectMapper().readTree(input));
    } catch (IOException | RuntimeException ex) {
      log.error(
          "Falha ao carregar versões de executores; modulo=agentmonitor recurso={}", RESOURCE, ex);
      throw new IllegalStateException(
          "Não foi possível validar as versões técnicas dos executores.", ex);
    }
  }

  /** Retorna a versão técnica exigida; ausência de catálogo nunca equivale a compatibilidade. */
  Integer expectedVersion(String agentKey) {
    return agentKey == null ? null : versions.get(agentKey);
  }
}
