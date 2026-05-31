package com.marketinghub.geralanding.imagegeneration.web;

import com.marketinghub.geralanding.imagegeneration.service.BackendImageGenerationService;
import com.marketinghub.geralanding.imagegeneration.service.listStageExecutions.GeraLandingImageGenerationExecutionSummaryResponse;
import com.marketinghub.geralanding.imagegeneration.service.GeraLandingImageGenerationStartResponse;
import com.marketinghub.geralanding.imagegeneration.service.detailStageExecution.RecordBackendImageGenerationDetalheDto;
import com.marketinghub.geralanding.imagegeneration.service.recebePrompt.RecebePromptRequest;
import com.marketinghub.geralanding.imagegeneration.service.recebeResposta.RecebeRespostaRequest;
import com.marketinghub.geralanding.imagegeneration.service.pending.RecordImageGenerationPending;
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

/** Responsável por expor os endpoints de backend da etapa landing-page-image-generation no GeraLanding. */
@RestController
@RequestMapping("/api")
public class BackendImageGenerationController {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendImageGenerationController.class);
  private static final String STAGE_CODE = "landing-page-image-generation";

  private final BackendImageGenerationService executionService;

  /** Inicializa o controller com o serviço backend da etapa image generation. */
  public BackendImageGenerationController(BackendImageGenerationService executionService) {
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-image-generation. */
  @PostMapping("/experiments/{experimentId}/geralanding/image-generation/start")
  public ResponseEntity<GeraLandingImageGenerationStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingImageGenerationStartResponse response = executionService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/experiments/{experimentId}/geralanding/image-generation/stage-executions")
  public ResponseEntity<List<GeraLandingImageGenerationExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingImageGenerationExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Lista os jobs pendentes iniciados da etapa image generation para processamento pelo Worker AI. */
  @GetMapping("/internal/geralanding/image-generation/stage-executions/pending")
  public List<RecordImageGenerationPending> pending() {
    return executionService.listPending(STAGE_CODE);
  }

  /** Recebe prompt, schema e request cru enviados para IA e marca a execução aguardando retorno da OpenAI. */
  @PostMapping("/internal/geralanding/image-generation/stage-executions/{idJob}/recebe-prompt")
  public ResponseEntity<Void> recebePrompt(
      @PathVariable String idJob, @Valid @RequestBody RecebePromptRequest payload) {
    LOGGER.info(
        "[GeraLanding][ImageGeneration] Recebido request enviado para IA idJob={} jobidopenai={} promptLength={} promptMarkdownLength={} schemaLength={} requestBodyLength={}",
        idJob,
        payload.jobidopenai(),
        payload.prompt().length(),
        payload.promptMarkdownContent() != null ? payload.promptMarkdownContent().length() : 0,
        payload.schemaJson().length(),
        payload.requestBodyJson().length());
    executionService.markWaitingOpenAiDispatch(
        idJob,
        payload.prompt(),
        payload.promptMarkdownContent(),
        payload.schemaJson(),
        payload.requestBodyJson(),
        payload.jobidopenai());
    return ResponseEntity.accepted().build();
  }

  /** Recebe a resposta da IA para a etapa image generation e conclui a execução do job. */
  @PostMapping("/internal/geralanding/image-generation/stage-executions/{idJob}/recebe-resposta")
  public ResponseEntity<Void> recebeResposta(
      @PathVariable String idJob, @Valid @RequestBody RecebeRespostaRequest payload) {
    LOGGER.info(
        "[GeraLanding][ImageGeneration] Recebida resposta da IA idJob={} experimentId={} stageCode={} openAiJobId={} inputTokens={} outputTokens={} costUsd={} hasError={}",
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

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/experiments/{experimentId}/geralanding/image-generation/stage-executions/{idJob}")
  public ResponseEntity<RecordBackendImageGenerationDetalheDto> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    RecordBackendImageGenerationDetalheDto response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
