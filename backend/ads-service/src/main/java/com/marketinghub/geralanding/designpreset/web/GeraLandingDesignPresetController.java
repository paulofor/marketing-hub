package com.marketinghub.geralanding.designpreset.web;

import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetExecutionSummaryResponse;
import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStageExecutionDetailResponse;
import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStageExecutionService;
import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStageService;
import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStartResponse;
import com.marketinghub.geralanding.designpreset.service.RecebePromptRequest;
import com.marketinghub.geralanding.designpreset.service.RecebeRespostaRequest;
import com.marketinghub.geralanding.designpreset.service.RecordDesignPresetPending;
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

/** Responsável por iniciar a etapa landing-page-design-preset no GeraLanding e expor consultas das execuções da etapa. */
@RestController
@RequestMapping("/api")
public class GeraLandingDesignPresetController {
  private static final Logger LOGGER = LoggerFactory.getLogger(GeraLandingDesignPresetController.class);
  private static final String STAGE_CODE = "landing-page-design-preset";

  private final GeraLandingDesignPresetStageService stageService;
  private final GeraLandingDesignPresetStageExecutionService executionService;

  /** Inicializa o controller com os serviços de início e execução da etapa design preset. */
  public GeraLandingDesignPresetController(GeraLandingDesignPresetStageService stageService, GeraLandingDesignPresetStageExecutionService executionService) {
    this.stageService = stageService;
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-design-preset. */
  @PostMapping("/experiments/{experimentId}/geralanding/design-preset/start")
  public ResponseEntity<GeraLandingDesignPresetStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingDesignPresetStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/experiments/{experimentId}/geralanding/design-preset/stage-executions")
  public ResponseEntity<List<GeraLandingDesignPresetExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingDesignPresetExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Lista os jobs pendentes iniciados da etapa design preset para processamento pelo Worker AI. */
  @GetMapping("/internal/geralanding/design-preset/stage-executions/pending")
  public List<RecordDesignPresetPending> pending() {
    return executionService.listPending(STAGE_CODE);
  }

  /** Recebe prompt, schema e request cru enviados para IA pelo Worker AI. */
  @PostMapping("/internal/geralanding/design-preset/stage-executions/{idJob}/recebe-prompt")
  public ResponseEntity<Void> recebePrompt(
      @PathVariable String idJob, @Valid @RequestBody RecebePromptRequest payload) {
    LOGGER.info(
        "[GeraLanding][DesignPreset] Recebido request enviado para IA idJob={} experimentId={} stageCode={} openAiJobId={} promptLength={} promptMarkdownLength={} schemaLength={} requestBodyLength={}",
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

  /** Recebe a resposta da IA para a etapa design preset e conclui ou falha a execução do job. */
  @PostMapping("/internal/geralanding/design-preset/stage-executions/{idJob}/recebe-resposta")
  public ResponseEntity<Void> recebeResposta(
      @PathVariable String idJob, @Valid @RequestBody RecebeRespostaRequest payload) {
    LOGGER.info(
        "[GeraLanding][DesignPreset] Recebida resposta da IA idJob={} experimentId={} stageCode={} openAiJobId={} inputTokens={} outputTokens={} costUsd={} hasError={}",
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
  @GetMapping("/experiments/{experimentId}/geralanding/design-preset/stage-executions/{idJob}")
  public ResponseEntity<GeraLandingDesignPresetStageExecutionDetailResponse> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    GeraLandingDesignPresetStageExecutionDetailResponse response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
