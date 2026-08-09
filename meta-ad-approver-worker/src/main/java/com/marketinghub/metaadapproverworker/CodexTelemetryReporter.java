package com.marketinghub.metaadapproverworker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: publicar a atividade auditável da sandbox Codex do Aprovador Meta. */
@Component
public class CodexTelemetryReporter {
  private static final Logger log = LoggerFactory.getLogger(CodexTelemetryReporter.class);
  private final RestClient backend;
  private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

  /** Configura o destino genérico de telemetria dos agentes Codex. */
  public CodexTelemetryReporter(MetaAdApproverProperties properties) {
    backend = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
  }

  /** Inicia o acompanhamento correlacionado ao criativo em revisão. */
  public Session monitor(Long creativeId, Process process, Path output) {
    return new Session(creativeId, process, output);
  }

  /** Responsabilidade: controlar heartbeats e encerramento de uma execução. */
  public final class Session implements AutoCloseable {
    private final Long creativeId;
    private final Process process;
    private final Path output;
    private final ScheduledFuture<?> task;
    private boolean success;

    /** Inicializa o heartbeat periódico. */
    private Session(Long creativeId, Process process, Path output) {
      this.creativeId = creativeId;
      this.process = process;
      this.output = output;
      task = timer.scheduleAtFixedRate(() -> send("heartbeat", false), 0, 15, TimeUnit.SECONDS);
    }

    /** Confirma conclusão funcional validada. */
    public void success() {
      success = true;
    }

    /** Envia a medição terminal. */
    @Override
    public void close() {
      task.cancel(false);
      send("finish", true);
    }

    /** Publica contadores técnicos sem transformar telemetria em resultado comercial. */
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
                "/api/codex-agent-telemetry/v1/internal/META_AD_APPROVER/executions/{id}/{action}",
                creativeId,
                action)
            .body(body)
            .retrieve()
            .toBodilessEntity();
      } catch (Exception ex) {
        log.warn("Falha na telemetria Codex do Aprovador Meta. creativeId={}", creativeId, ex);
      }
    }
  }
}
