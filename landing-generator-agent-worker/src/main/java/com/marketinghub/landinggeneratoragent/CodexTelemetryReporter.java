package com.marketinghub.landinggeneratoragent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: publicar atividade auditável do processo Codex do Agente de Landing. */
@Component
public class CodexTelemetryReporter {
  private static final Logger log = LoggerFactory.getLogger(CodexTelemetryReporter.class);
  private final RestClient backend;
  private final ObjectMapper objectMapper;
  private final java.util.concurrent.ScheduledExecutorService timer =
      Executors.newSingleThreadScheduledExecutor();

  /** Configura o destino central de telemetria. */
  public CodexTelemetryReporter(
      LandingGeneratorAgentProperties properties, ObjectMapper objectMapper) {
    backend = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
    this.objectMapper = objectMapper;
  }

  /** Inicia heartbeats correlacionados ao experimento. */
  public Session monitor(LandingAgentJob job, Process process, Path output) {
    return new Session(executionTelemetryId(job.executionId()), process, output);
  }

  /** Deriva uma identidade estável por execução para somar tentativas distintas no total diário. */
  long executionTelemetryId(String executionId) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(executionId.getBytes(StandardCharsets.UTF_8));
      long value = 0;
      for (int index = 0; index < Long.BYTES; index++) {
        value = (value << 8) | Byte.toUnsignedLong(digest[index]);
      }
      return value & Long.MAX_VALUE;
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível para correlacionar telemetria", ex);
    }
  }

  /** Responsabilidade: encerrar corretamente a telemetria de uma execução. */
  public final class Session implements AutoCloseable {
    private final Long experimentId;
    private final Process process;
    private final Path output;
    private final ScheduledFuture<?> task;
    private boolean success;

    /** Inicia o heartbeat a cada quinze segundos. */
    private Session(Long experimentId, Process process, Path output) {
      this.experimentId = experimentId;
      this.process = process;
      this.output = output;
      task = timer.scheduleAtFixedRate(() -> send(false), 0, 15, TimeUnit.SECONDS);
    }

    /** Confirma uma conclusão funcional validada. */
    public void success() {
      success = true;
    }

    /** Recupera a medição real mais recente produzida pelo Codex. */
    public TokenUsage tokenUsage() {
      return readTokenUsage(output);
    }

    /** Publica o encerramento terminal. */
    @Override
    public void close() {
      task.cancel(false);
      send(true);
    }

    /** Envia medições sem inventar tokens ausentes. */
    private void send(boolean terminal) {
      try {
        Map<String, Object> body = new HashMap<>();
        long bytes = Files.exists(output) ? Files.size(output) : 0;
        body.put("processId", process.pid());
        body.put("processAlive", process.isAlive());
        body.put("eventCount", Files.exists(output) ? Files.lines(output).count() : 0);
        body.put("outputBytes", bytes);
        body.put("lastEventType", bytes > 0 ? "OUTPUT" : "HEARTBEAT");
        TokenUsage usage = readTokenUsage(output);
        body.put("inputTokens", usage.inputTokens());
        body.put("outputTokens", usage.outputTokens());
        if (terminal) body.put("success", success);
        backend
            .post()
            .uri(
                "/api/codex-agent-telemetry/v1/internal/LANDING_GENERATOR/executions/{id}/{action}",
                experimentId,
                terminal ? "finish" : "heartbeat")
            .body(body)
            .retrieve()
            .toBodilessEntity();
      } catch (Exception ex) {
        log.warn("Falha na telemetria do Agente de Landing. experimentId={}", experimentId, ex);
      }
    }
  }

  /** Lê eventos JSONL e preserva a última contagem cumulativa informada pelo Codex. */
  TokenUsage readTokenUsage(Path output) {
    if (!Files.exists(output)) return TokenUsage.empty();
    long inputTokens = 0;
    long outputTokens = 0;
    boolean informed = false;
    try {
      for (String line : Files.readAllLines(output)) {
        if (line.isBlank()) continue;
        JsonNode event;
        try {
          event = objectMapper.readTree(line);
        } catch (IOException ignored) {
          continue;
        }
        JsonNode usage = event.path("usage");
        if (!usage.isObject()) continue;
        Long input = tokenValue(usage, "input_tokens", "inputTokens");
        Long outputValue = tokenValue(usage, "output_tokens", "outputTokens");
        if (input != null || outputValue != null) {
          informed = true;
          inputTokens = Math.max(inputTokens, input == null ? 0 : input);
          outputTokens = Math.max(outputTokens, outputValue == null ? 0 : outputValue);
        }
      }
    } catch (IOException ex) {
      log.warn("Falha ao ler tokens reais da saída JSONL do Codex. output={}", output, ex);
      return TokenUsage.empty();
    }
    return informed ? new TokenUsage(inputTokens, outputTokens) : TokenUsage.empty();
  }

  /** Aceita o nome oficial JSON e o alias Java sem estimar valores ausentes. */
  private Long tokenValue(JsonNode usage, String officialName, String alias) {
    JsonNode value = usage.hasNonNull(officialName) ? usage.get(officialName) : usage.get(alias);
    return value != null && value.isNumber() ? value.longValue() : null;
  }

  /** Representa a medição real de tokens informada pelo Codex. */
  public record TokenUsage(Long inputTokens, Long outputTokens) {
    /** Representa uma execução que ainda não informou consumo. */
    static TokenUsage empty() {
      return new TokenUsage(null, null);
    }
  }
}
