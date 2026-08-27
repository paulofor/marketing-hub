package com.marketinghub.landinggeneratoragent;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: consumir fila e callbacks exclusivamente pelo backend. */
@Component
public class LandingGeneratorBackendClient {
  private static final Logger log = LoggerFactory.getLogger(LandingGeneratorBackendClient.class);
  private final LandingGeneratorAgentProperties properties;
  private final RestClient client;

  /** Configura o backend como única porta de estado. */
  public LandingGeneratorBackendClient(
      LandingGeneratorAgentProperties properties, RestClient.Builder builder) {
    this.properties = properties;
    client =
        builder
            .baseUrl(properties.getBackendUrl())
            .defaultHeader("X-Agent-Build-Reference", properties.getBuildReference())
            .build();
  }

  /** Reserva no máximo uma landing para preservar custo e isolamento. */
  public List<LandingAgentJob> claimPending() {
    activatePendingProcessTask();
    List<Map<String, Object>> values =
        client
            .get()
            .uri("/api/internal/geralanding/agent/v1/stage-executions/pending?limit=1")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    return values == null ? List.of() : values.stream().map(LandingAgentJob::from).toList();
  }

  /** Reserva e materializa a atividade BPM liberada antes de consultar a fila técnica. */
  void activatePendingProcessTask() {
    client
        .post()
        .uri("/api/internal/geralanding/agent/v1/stage-executions/process-tasks/pending/activation")
        .retrieve()
        .toBodilessEntity();
  }

  /** Persiste o resultado sem decidir a próxima etapa localmente. */
  public void report(LandingAgentJob job, Map<String, Object> result) {
    client
        .post()
        .uri("/api/internal/geralanding/agent/v1/stage-executions/{id}/result", job.executionId())
        .body(result)
        .retrieve()
        .toBodilessEntity();
  }

  /** Registra falha técnica mantendo publicação e pipeline bloqueados. */
  public void fail(LandingAgentJob job, RuntimeException error) {
    log.error(
        "Falha no Agente Gerador de Landing. experimentId={} executionId={}",
        job.experimentId(),
        job.executionId(),
        error);
    report(
        job,
        Map.of(
            "decisionJson",
            "{}",
            "requestJson",
            "falha antes da conclusão",
            "responseJson",
            "{}",
            "model",
            properties.getModel(),
            "reasoningEffort",
            properties.requiredReasoningEffort(),
            "error",
            rootMessage(error)));
  }

  /** Reserva uma solicitação de reconexão pelo pending canônico do executor. */
  public CodexAuthReconnectJob claimCodexAuthReconnect() {
    ResponseEntity<CodexAuthReconnectJob> response =
        client
            .get()
            .uri(
                "/api/internal/agents/executor-health/landing-generator/codex-auth/reconnections/pending")
            .retrieve()
            .toEntity(CodexAuthReconnectJob.class);
    return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
  }

  /** Registra uma falha local que ocorreu antes do callback do App Server. */
  public void completeCodexAuth(Long id, boolean authenticated, String detail) {
    client
        .post()
        .uri("/api/internal/agents/executor-health/codex-auth/reconnections/{id}/completion", id)
        .body(Map.of("authenticated", authenticated, "detail", detail))
        .retrieve()
        .toBodilessEntity();
  }

  /** Extrai a causa específica preservada integralmente no log. */
  private String rootMessage(Throwable error) {
    Throwable current = error;
    while (current.getCause() != null) current = current.getCause();
    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }
}
