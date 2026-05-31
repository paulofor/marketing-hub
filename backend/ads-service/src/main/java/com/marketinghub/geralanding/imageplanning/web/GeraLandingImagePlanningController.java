package com.marketinghub.geralanding.imageplanning.web;

import com.marketinghub.geralanding.imageplanning.service.BackendImagePlanningService;
import com.marketinghub.geralanding.imageplanning.service.GeraLandingImagePlanningStartResponse;
import com.marketinghub.geralanding.imageplanning.service.detailStageExecution.RecordBackendImagePlanningDetalheDto;
import com.marketinghub.geralanding.imageplanning.service.listStageExecutions.GeraLandingImagePlanningExecutionSummaryResponse;
import com.marketinghub.geralanding.imageplanning.service.pending.RecordImagePlanningPending;
import com.marketinghub.geralanding.imageplanning.service.recebePrompt.RecebeDispatchRequest;
import com.marketinghub.geralanding.imageplanning.service.recebePrompt.RecebePromptRequest;
import com.marketinghub.geralanding.imageplanning.service.recebeResposta.RecebeRespostaRequest;
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

/** Responsável por expor os endpoints de backend da etapa landing-page-image-planning no GeraLanding. */
@RestController
@RequestMapping("/api")
public class GeraLandingImagePlanningController {
  private static final Logger LOGGER = LoggerFactory.getLogger(GeraLandingImagePlanningController.class);
  private static final String STAGE_CODE = "landing-page-image-planning";

  private final BackendImagePlanningService executionService;

  /** Inicializa o controller com o serviço backend da etapa image planning. */
  public GeraLandingImagePlanningController(BackendImagePlanningService executionService) {
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-image-planning. */
  @PostMapping("/experiments/{experimentId}/geralanding/image-prompts/start")
  public ResponseEntity<GeraLandingImagePlanningStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingImagePlanningStartResponse response = executionService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/experiments/{experimentId}/geralanding/image-prompts/stage-executions")
  public ResponseEntity<List<GeraLandingImagePlanningExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingImagePlanningExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Lista os jobs pendentes iniciados da etapa image planning para processamento pelo Worker AI. */
  @GetMapping("/internal/geralanding/image-prompts/stage-executions/pending")
  public List<RecordImagePlanningPending> pending() {
    return executionService.listPending(STAGE_CODE);
  }

  /** Recebe prompt, schema e request cru enviados para IA. */
  @PostMapping("/internal/geralanding/image-prompts/stage-executions/{idJob}/receive-prompt")
  public ResponseEntity<Void> receivePrompt(
      @PathVariable String idJob, @Valid @RequestBody RecebePromptRequest payload) {
    LOGGER.info(
        "[GeraLanding][ImagePlanning] Recebido request enviado para IA idJob={} experimentId={} stageCode={} openAiJobId={} promptLength={} promptMarkdownLength={} schemaLength={} requestBodyLength={}",
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
  @PostMapping("/internal/geralanding/image-prompts/stage-executions/{idJob}/recebe-prompt")
  public ResponseEntity<Void> recebePrompt(
      @PathVariable String idJob, @Valid @RequestBody RecebePromptRequest payload) {
    return receivePrompt(idJob, payload);
  }

  /** Recebe o identificador remoto da IA e marca a execução aguardando retorno da OpenAI. */
  @PostMapping("/internal/geralanding/image-prompts/stage-executions/{idJob}/receive-dispatch")
  public ResponseEntity<Void> receiveDispatch(
      @PathVariable String idJob, @RequestBody RecebeDispatchRequest payload) {
    LOGGER.info(
        "[GeraLanding][ImagePlanning] Recebido dispatch OpenAI idJob={} experimentId={} stageCode={} openAiJobId={}",
        idJob,
        payload.experimentId(),
        payload.stageCode(),
        payload.openAiJobId());
    executionService.markWaitingOpenAiDispatch(idJob, payload.openAiJobId());
    return ResponseEntity.accepted().build();
  }

  /** Recebe a resposta da IA para a etapa image planning e conclui a execução do job. */
  @PostMapping("/internal/geralanding/image-prompts/stage-executions/{idJob}/receive-result")
  public ResponseEntity<Void> receiveResult(
      @PathVariable String idJob, @Valid @RequestBody RecebeRespostaRequest payload) {
    LOGGER.info(
        "[GeraLanding][ImagePlanning] Recebida resposta da IA idJob={} experimentId={} stageCode={} openAiJobId={} inputTokens={} outputTokens={} costUsd={} hasError={}",
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
        payload.provisionalHtml(),
        payload.inputTokens(),
        payload.outputTokens(),
        payload.costUsd(),
        payload.openAiJobId(),
        payload.errorMessage(),
        payload.errorDetail());
    return ResponseEntity.accepted().build();
  }

  /** Recebe a resposta da IA para a etapa image planning pelo contrato em português. */
  @PostMapping("/internal/geralanding/image-prompts/stage-executions/{idJob}/recebe-resposta")
  public ResponseEntity<Void> recebeResposta(
      @PathVariable String idJob, @Valid @RequestBody RecebeRespostaRequest payload) {
    return receiveResult(idJob, payload);
  }

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/experiments/{experimentId}/geralanding/image-prompts/stage-executions/{idJob}")
  public ResponseEntity<RecordBackendImagePlanningDetalheDto> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    RecordBackendImagePlanningDetalheDto response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
