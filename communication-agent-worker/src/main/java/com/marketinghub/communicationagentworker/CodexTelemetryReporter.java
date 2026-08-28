package com.marketinghub.communicationagentworker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: reportar ao backend a atividade auditável do processo Codex de Íris. */
@Component
public class CodexTelemetryReporter {
  private static final Logger log = LoggerFactory.getLogger(CodexTelemetryReporter.class);
  private final RestClient backend;
  private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

  /** Configura o destino canônico da telemetria. */
  public CodexTelemetryReporter(CommunicationAgentProperties properties) {
    backend = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
  }

  /** Monitora uma execução a cada quinze segundos. */
  public Session monitor(long taskId, Process process, Path output) {
    return new Session(taskId, process, output);
  }

  /** Responsabilidade: controlar o ciclo de telemetria de uma tarefa de Íris. */
  public final class Session implements AutoCloseable {
    private final long taskId;
    private final Process process;
    private final Path output;
    private final ScheduledFuture<?> heartbeat;
    private boolean success;

    /** Inicia o heartbeat periódico da execução reservada. */
    private Session(long taskId, Process process, Path output) {
      this.taskId = taskId;
      this.process = process;
      this.output = output;
      heartbeat =
          timer.scheduleAtFixedRate(() -> send("heartbeat", false), 0, 15, TimeUnit.SECONDS);
    }

    /** Confirma que o resultado passou pelo contrato funcional. */
    public void success() {
      success = true;
    }

    /** Encerra a medição com o último estado conhecido. */
    @Override
    public void close() {
      heartbeat.cancel(false);
      send("finish", true);
    }

    /** Publica vida do processo e volume de eventos sem estimar tokens. */
    private void send(String action, boolean terminal) {
      try {
        long bytes = Files.exists(output) ? Files.size(output) : 0;
        long events = Files.exists(output) ? Files.readAllLines(output).size() : 0;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("processId", process.pid());
        body.put("processAlive", process.isAlive());
        body.put("eventCount", events);
        body.put("outputBytes", bytes);
        body.put("lastEventType", bytes > 0 ? "OUTPUT" : "HEARTBEAT");
        if (terminal) body.put("success", success);
        backend
            .post()
            .uri(
                "/api/codex-agent-telemetry/v1/internal/COMMUNICATION_DIRECTOR/executions/{id}/{action}",
                taskId,
                action)
            .body(body)
            .retrieve()
            .toBodilessEntity();
      } catch (Exception ex) {
        log.warn("Falha na telemetria Codex de Íris. taskId={}", taskId, ex);
      }
    }
  }
}
