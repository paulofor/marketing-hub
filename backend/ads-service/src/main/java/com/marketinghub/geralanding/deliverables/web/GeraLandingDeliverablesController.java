package com.marketinghub.geralanding.deliverables.web;

import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesExecutionSummaryResponse;
import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesStageExecutionDetailResponse;
import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesStageExecutionService;
import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesStageService;
import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesStartResponse;
import com.marketinghub.geralanding.deliverables.service.pending.RecordDeliverablesPending;
import com.marketinghub.geralanding.deliverables.service.recebePrompt.RecebeDispatchRequest;
import com.marketinghub.geralanding.deliverables.service.recebePrompt.RecebePromptRequest;
import com.marketinghub.geralanding.deliverables.service.recebeResposta.RecebeRespostaRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-deliverables no GeraLanding e expor consultas/callbacks da etapa. */
@RestController
@RequestMapping("/api")
public class GeraLandingDeliverablesController {
  private static final Logger LOGGER = LoggerFactory.getLogger(GeraLandingDeliverablesController.class);
  private static final String STAGE_CODE = "landing-page-deliverables";

  private final GeraLandingDeliverablesStageService stageService;
  private final GeraLandingDeliverablesStageExecutionService executionService;

  /** Inicializa o controller com os serviços de início e execução da etapa deliverables. */
  public GeraLandingDeliverablesController(GeraLandingDeliverablesStageService stageService, GeraLandingDeliverablesStageExecutionService executionService) {
    this.stageService = stageService;
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-deliverables. */
  @PostMapping("/experiments/{experimentId}/geralanding/deliverables/start")
  public ResponseEntity<GeraLandingDeliverablesStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingDeliverablesStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/experiments/{experimentId}/geralanding/deliverables/stage-executions")
  public ResponseEntity<List<GeraLandingDeliverablesExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingDeliverablesExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Lista os jobs pendentes iniciados da etapa deliverables para processamento pelo Worker AI. */
  @GetMapping("/internal/geralanding/deliverables/stage-executions/pending")
  public List<RecordDeliverablesPending> pending() {
    return executionService.listPending(STAGE_CODE);
  }

  /** Marca uma execução como em processamento para evitar recaptura enquanto o Worker AI chama a OpenAI. */
  @PostMapping("/internal/geralanding/deliverables/stage-executions/{idJob}/running")
  public ResponseEntity<Void> running(@PathVariable String idJob, @RequestBody(required = false) RecebeDispatchRequest payload) {
    LOGGER.info(
        "[GeraLanding][Deliverables] Marcando execução em processamento idJob={} experimentId={} stageCode={}",
        idJob,
        payload != null ? payload.experimentId() : null,
        payload != null ? payload.stageCode() : null);
    executionService.markRunning(idJob);
    return ResponseEntity.accepted().build();
  }

  /** Recebe prompt, schema e request cru enviados para IA. */
  @PostMapping("/internal/geralanding/deliverables/stage-executions/{idJob}/receive-prompt")
  public ResponseEntity<Void> receivePrompt(
      @PathVariable String idJob, @Valid @RequestBody RecebePromptRequest payload) {
    LOGGER.info(
        "[GeraLanding][Deliverables] Recebido request enviado para IA idJob={} experimentId={} stageCode={} openAiJobId={} promptLength={} promptMarkdownLength={} schemaLength={} requestBodyLength={}",
        idJob,
        payload.experimentId(),
        payload.stageCode(),
        payload.openAiJobId(),
        payload.prompt().length(),
        payload.promptMarkdownContent() != null ? payload.promptMarkdownContent().length() : 0,
        payload.schemaJson().length(),
        payload.openAiRequestBody().length());
    executionService.markPromptReceived(
        idJob,
        payload.prompt(),
        payload.promptMarkdownContent(),
        payload.schemaJson(),
        payload.openAiRequestBody(),
        payload.openAiModel(),
        payload.openAiJobId());
    return ResponseEntity.accepted().build();
  }

  /** Recebe prompt, schema e request cru enviados para IA pelo contrato em português. */
  @PostMapping("/internal/geralanding/deliverables/stage-executions/{idJob}/recebe-prompt")
  public ResponseEntity<Void> recebePrompt(
      @PathVariable String idJob, @Valid @RequestBody RecebePromptRequest payload) {
    return receivePrompt(idJob, payload);
  }

  /** Recebe o identificador remoto da IA e marca a execução aguardando retorno da OpenAI. */
  @PostMapping("/internal/geralanding/deliverables/stage-executions/{idJob}/receive-dispatch")
  public ResponseEntity<Void> receiveDispatch(
      @PathVariable String idJob, @RequestBody RecebeDispatchRequest payload) {
    LOGGER.info(
        "[GeraLanding][Deliverables] Recebido dispatch OpenAI idJob={} experimentId={} stageCode={} openAiJobId={}",
        idJob,
        payload.experimentId(),
        payload.stageCode(),
        payload.openAiJobId());
    executionService.markWaitingOpenAiDispatch(idJob, payload.openAiJobId());
    return ResponseEntity.accepted().build();
  }

  /** Recebe a resposta da IA para a etapa deliverables e conclui a execução do job. */
  @PostMapping("/internal/geralanding/deliverables/stage-executions/{idJob}/receive-result")
  public ResponseEntity<Void> receiveResult(
      @PathVariable String idJob, @Valid @RequestBody RecebeRespostaRequest payload) {
    LOGGER.info(
        "[GeraLanding][Deliverables] Recebida resposta da IA idJob={} experimentId={} stageCode={} openAiJobId={} inputTokens={} outputTokens={} costUsd={} hasError={}",
        idJob,
        payload.experimentId(),
        payload.stageCode(),
        payload.openAiJobId(),
        payload.inputTokens(),
        payload.outputTokens(),
        payload.costUsd(),
        payload.errorMessage() != null && !payload.errorMessage().isBlank());
    executionService.markCompletedFromResponse(
        idJob,
        payload.experimentId(),
        payload.stageCode(),
        payload.modelResponse(),
        payload.inputTokens(),
        payload.outputTokens(),
        payload.costUsd(),
        payload.openAiJobId(),
        payload.errorMessage(),
        payload.errorDetail());
    return ResponseEntity.accepted().build();
  }

  /** Recebe a resposta da IA para a etapa deliverables pelo contrato em português. */
  @PostMapping("/internal/geralanding/deliverables/stage-executions/{idJob}/recebe-resposta")
  public ResponseEntity<Void> recebeResposta(
      @PathVariable String idJob, @Valid @RequestBody RecebeRespostaRequest payload) {
    return receiveResult(idJob, payload);
  }

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/experiments/{experimentId}/geralanding/deliverables/stage-executions/{idJob}")
  public ResponseEntity<GeraLandingDeliverablesStageExecutionDetailResponse> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    GeraLandingDeliverablesStageExecutionDetailResponse response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
