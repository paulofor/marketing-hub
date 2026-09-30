package com.marketinghub.metaadapproverworker;

import jakarta.annotation.PreDestroy;
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

/** Responsabilidade: publicar a atividade auditável da sandbox Codex executada por Têmis. */
@Component
public class CodexTelemetryReporter {
  private static final Logger log = LoggerFactory.getLogger(CodexTelemetryReporter.class);
  private static final String CREATIVE_AGENT_TYPE = "META_AD_APPROVER";
  private static final String BPM_AGENT_TYPE = "TEMIS_BPM";
  private final RestClient backend;
  private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

  /** Configura o destino genérico de telemetria dos agentes Codex. */
  public CodexTelemetryReporter(MetaAdApproverProperties properties) {
    backend = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
  }

  /** Inicia o acompanhamento correlacionado ao criativo em revisão. */
  public Session monitor(Long creativeId, Process process, Path output) {
    return new Session(CREATIVE_AGENT_TYPE, creativeId, process, output);
  }

  /** Inicia o acompanhamento correlacionado à tarefa comercial BPM de Têmis. */
  public Session monitorBpmTask(Long taskId, Process process, Path output) {
    return new Session(BPM_AGENT_TYPE, taskId, process, output);
  }

  /** Encerra o agendador de heartbeat durante a parada controlada do container. */
  @PreDestroy
  public void shutdown() {
    timer.shutdownNow();
  }

  /** Responsabilidade: controlar heartbeats e encerramento de uma execução. */
  public final class Session implements AutoCloseable {
    private final String agentType;
    private final Long executionId;
    private final Process process;
    private final Path output;
    private final ScheduledFuture<?> task;
    private boolean success;

    /** Inicializa o heartbeat periódico. */
    private Session(String agentType, Long executionId, Process process, Path output) {
      this.agentType = agentType;
      this.executionId = executionId;
      this.process = process;
      this.output = output;
      task = timer.scheduleAtFixedRate(() -> send("heartbeat", false), 0, 15, TimeUnit.SECONDS);
    }

    /** Confirma conclusão funcional validada pelo contrato da atividade. */
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
        long events = countEvents(output);
        body.put("processId", process.pid());
        body.put("processAlive", process.isAlive());
        body.put("eventCount", events);
        body.put("outputBytes", bytes);
        body.put("lastEventType", bytes > 0 ? "OUTPUT" : "HEARTBEAT");
        if (terminal) body.put("success", success);
        backend
            .post()
            .uri(
                "/api/codex-agent-telemetry/v1/internal/{agentType}/executions/{id}/{action}",
                agentType,
                executionId,
                action)
            .body(body)
            .retrieve()
            .toBodilessEntity();
      } catch (Exception ex) {
        log.warn(
            "Falha na telemetria Codex de Têmis. agentType={} executionId={}",
            agentType,
            executionId,
            ex);
      }
    }
  }

  /** Conta eventos fechando o descritor a cada heartbeat para não esgotar o worker. */
  private long countEvents(Path output) throws java.io.IOException {
    if (!Files.exists(output)) return 0L;
    try (var lines = Files.lines(output)) {
      return lines.count();
    }
  }
}
