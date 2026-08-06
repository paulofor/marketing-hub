package com.marketinghub.experimentstrategistworker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: enviar ao backend a atividade auditável do Codex Estrategista. */
@Component
public class CodexTelemetryReporter {
  private static final Logger log = LoggerFactory.getLogger(CodexTelemetryReporter.class);
  private final RestClient backend;
  private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

  /** Configura o destino canônico. */
  public CodexTelemetryReporter(WorkerProperties p) {
    backend = RestClient.builder().baseUrl(p.getBackendUrl()).build();
  }

  /** Monitora uma execução a cada quinze segundos. */
  public Session monitor(Long id, Process process, Path output) {
    return new Session(id, process, output);
  }

  /** Responsabilidade: controlar o ciclo de telemetria de uma execução. */
  public final class Session implements AutoCloseable {
    private final Long id;
    private final Process process;
    private final Path output;
    private final ScheduledFuture<?> task;
    private boolean success;

    private Session(Long id, Process process, Path output) {
      this.id = id;
      this.process = process;
      this.output = output;
      task = timer.scheduleAtFixedRate(this::heartbeat, 0, 15, TimeUnit.SECONDS);
    }

    /** Confirma sucesso funcional. */
    public void success() {
      success = true;
    }

    /** Envia heartbeat tolerante a falha. */
    private void heartbeat() {
      send("heartbeat", false);
    }

    /** Encerra a medição. */
    @Override
    public void close() {
      task.cancel(false);
      send("finish", true);
    }

    /** Publica processo, eventos e bytes sem estimar tokens. */
    private void send(String action, boolean terminal) {
      try {
        Map<String, Object> body = new HashMap<>();
        long bytes = Files.exists(output) ? Files.size(output) : 0L;
        long events = Files.exists(output) ? Files.lines(output).count() : 0L;
        body.put("processId", process.pid());
        body.put("processAlive", process.isAlive());
        body.put("eventCount", events);
        body.put("outputBytes", bytes);
        body.put("lastEventType", bytes > 0 ? "OUTPUT" : "HEARTBEAT");
        if (terminal) body.put("success", success);
        backend
            .post()
            .uri(
                "/api/codex-agent-telemetry/v1/internal/EXPERIMENT_STRATEGIST/executions/{id}/{action}",
                id,
                action)
            .body(body)
            .retrieve()
            .toBodilessEntity();
      } catch (Exception ex) {
        log.warn("Falha na telemetria Codex do Estrategista; executionId={}", id, ex);
      }
    }
  }
}
