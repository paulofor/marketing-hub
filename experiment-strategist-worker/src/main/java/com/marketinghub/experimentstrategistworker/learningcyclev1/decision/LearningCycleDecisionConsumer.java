package com.marketinghub.experimentstrategistworker.learningcyclev1.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.experimentstrategistworker.AutomaticExecutionControl;
import com.marketinghub.experimentstrategistworker.WorkerProperties;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: consumir propostas do ciclo e reportar auditoria e resultado ao backend. */
@Component
public class LearningCycleDecisionConsumer {
  private static final Logger log = LoggerFactory.getLogger(LearningCycleDecisionConsumer.class);
  static final String ENDPOINT =
      "/api/internal/business-process-chains/learning-cycles/v1/decision/stage-executions";
  private final RestClient backend;
  private final LearningCycleDecisionRunner runner;
  private final AutomaticExecutionControl control;

  /** Configura a porta do próprio fluxo e o controle PLAY do executor de Atena. */
  public LearningCycleDecisionConsumer(
      WorkerProperties properties,
      LearningCycleDecisionRunner runner,
      AutomaticExecutionControl control) {
    var requests = new SimpleClientHttpRequestFactory();
    requests.setConnectTimeout(Duration.ofSeconds(5));
    requests.setReadTimeout(Duration.ofSeconds(30));
    backend =
        RestClient.builder().baseUrl(properties.getBackendUrl()).requestFactory(requests).build();
    this.runner = runner;
    this.control = control;
  }

  /** Reserva uma proposta por execução agendada; somente o backend governa o avanço comercial. */
  @Scheduled(cron = "25 */1 * * * *")
  public void processOne() {
    if (!control.allowsAutomaticExecution()) return;
    JsonNode job = null;
    LearningCycleDecisionRunner.Result result = null;
    try {
      JsonNode pending = backend.get().uri(ENDPOINT + "/pending").retrieve().body(JsonNode.class);
      if (pending == null || pending.isEmpty()) return;
      job = pending.get(0);
      log.info(
          "Atena: preparando proposta proposalId={} cycleId={} endpoint={}",
          job.path("proposalId"),
          job.path("cycleId"),
          ENDPOINT);
      var prepared = runner.prepare(job.path("context"));
      backend
          .put()
          .uri(ENDPOINT + "/{id}/request", job.path("proposalId").asLong())
          .body(
              Map.of(
                  "leaseToken",
                  job.path("leaseToken").asText(),
                  "prompt",
                  prepared.prompt(),
                  "schema",
                  prepared.schema(),
                  "model",
                  prepared.model(),
                  "serviceTier",
                  "default",
                  "serviceTierReason",
                  "Exceção do harness Codex OAuth versionado de Atena: mantém service_tier=default, como a estratégia BPM existente; Flex deve ser usado quando suportado pelo provedor deste runtime."))
          .retrieve()
          .toBodilessEntity();
      result = runner.run(prepared);
      callback(job, result);
    } catch (Exception ex) {
      log.error(
          "Atena: falha na proposta comercial proposalId={} cycleId={} endpoint={}",
          job == null ? null : job.path("proposalId"),
          job == null ? null : job.path("cycleId"),
          ENDPOINT,
          ex);
      if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
      if (job != null) {
        try {
          callback(
              job,
              result != null
                  ? result
                  : new LearningCycleDecisionRunner.Result(null, ex.toString(), null, null));
        } catch (Exception callbackEx) {
          log.error(
              "Atena: falha ao registrar resultado; lease e request permanecem auditáveis. proposalId={} endpoint={}",
              job.path("proposalId"),
              ENDPOINT,
              callbackEx);
        }
      }
    }
  }

  /** Devolve resposta bruta, falha e tokens sem acionar comando administrativo ou sucessor. */
  private void callback(JsonNode job, LearningCycleDecisionRunner.Result result) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("leaseToken", job.path("leaseToken").asText());
    body.put("rawResponse", result.rawResponse());
    body.put("error", result.error());
    body.put("inputTokens", result.inputTokens());
    body.put("outputTokens", result.outputTokens());
    body.put("costUsd", null);
    var receipt =
        backend
            .post()
            .uri(ENDPOINT + "/{id}/result", job.path("proposalId").asLong())
            .body(body)
            .retrieve()
            .body(JsonNode.class);
    log.info(
        "Atena: proposta registrada proposalId={} cycleId={} status={} endpoint={}",
        job.path("proposalId"),
        job.path("cycleId"),
        receipt == null ? null : receipt.path("status"),
        ENDPOINT);
  }
}
