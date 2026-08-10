package com.marketinghub.landinggeneratoragent;

import java.nio.file.Files;
import java.nio.file.Path;
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
  private final java.util.concurrent.ScheduledExecutorService timer =
      Executors.newSingleThreadScheduledExecutor();

  /** Configura o destino central de telemetria. */
  public CodexTelemetryReporter(LandingGeneratorAgentProperties properties) {
    backend = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
  }

  /** Inicia heartbeats correlacionados ao experimento. */
  public Session monitor(LandingAgentJob job, Process process, Path output) {
    return new Session(job.experimentId(), process, output);
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
}
