package com.marketinghub.geralanding.presetdesign.web;

import com.marketinghub.geralanding.presetdesign.service.BackendPresetDesignService;
import com.marketinghub.geralanding.presetdesign.service.listStageExecutions.GeraLandingPresetDesignExecutionSummaryResponse;
import com.marketinghub.geralanding.presetdesign.service.GeraLandingPresetDesignStartResponse;
import com.marketinghub.geralanding.presetdesign.service.detailStageExecution.RecordBackendPresetDesignDetalheDto;
import com.marketinghub.geralanding.presetdesign.service.recebePrompt.RecebeDispatchRequest;
import com.marketinghub.geralanding.presetdesign.service.recebePrompt.RecebePromptRequest;
import com.marketinghub.geralanding.presetdesign.service.recebeResposta.RecebeRespostaRequest;
import com.marketinghub.geralanding.presetdesign.service.pending.RecordPresetDesignPending;
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

/** Responsável por expor os endpoints de backend da etapa landing-page-design-preset no GeraLanding. */
@RestController
@RequestMapping("/api")
public class BackendPresetDesignController {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendPresetDesignController.class);
  private static final String STAGE_CODE = "landing-page-design-preset";

  private final BackendPresetDesignService executionService;

  /** Inicializa o controller com o serviço backend da etapa presetdesign. */
  public BackendPresetDesignController(BackendPresetDesignService executionService) {
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-design-preset. */
  @PostMapping("/experiments/{experimentId}/geralanding/design-preset/start")
  public ResponseEntity<GeraLandingPresetDesignStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingPresetDesignStartResponse response = executionService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/experiments/{experimentId}/geralanding/design-preset/stage-executions")
  public ResponseEntity<List<GeraLandingPresetDesignExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingPresetDesignExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Lista os jobs pendentes iniciados da etapa presetdesign para processamento pelo Worker AI. */
  @GetMapping("/internal/geralanding/design-preset/stage-executions/pending")
  public List<RecordPresetDesignPending> pending() {
    return executionService.listPending(STAGE_CODE);
  }

  /** Recebe prompt, schema e request cru enviados para IA e marca a execução aguardando retorno da OpenAI. */
  @PostMapping("/internal/geralanding/design-preset/stage-executions/{idJob}/receive-prompt")
  public ResponseEntity<Void> receivePrompt(
      @PathVariable String idJob, @Valid @RequestBody RecebePromptRequest payload) {
    LOGGER.info(
        "[GeraLanding][PresetDesign] Recebido request enviado para IA idJob={} jobidopenai={} promptLength={} promptMarkdownLength={} schemaLength={} requestBodyLength={}",
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

  /** Recebe prompt, schema e request cru enviados para IA pelo contrato em português. */
  @PostMapping("/internal/geralanding/design-preset/stage-executions/{idJob}/recebe-prompt")
  public ResponseEntity<Void> recebePrompt(
      @PathVariable String idJob, @Valid @RequestBody RecebePromptRequest payload) {
    return receivePrompt(idJob, payload);
  }

  /** Recebe o identificador remoto da IA e marca a execução aguardando retorno da OpenAI. */
  @PostMapping("/internal/geralanding/design-preset/stage-executions/{idJob}/receive-dispatch")
  public ResponseEntity<Void> receiveDispatch(
      @PathVariable String idJob, @RequestBody RecebeDispatchRequest payload) {
    LOGGER.info(
        "[GeraLanding][PresetDesign] Recebido dispatch OpenAI idJob={} experimentId={} stageCode={} openAiJobId={}",
        idJob,
        payload.experimentId(),
        payload.stageCode(),
        payload.openAiJobId());
    executionService.markWaitingOpenAiDispatch(idJob, payload.openAiJobId());
    return ResponseEntity.accepted().build();
  }

  /** Recebe a resposta da IA para a etapa presetdesign e conclui a execução do job. */
  @PostMapping("/internal/geralanding/design-preset/stage-executions/{idJob}/receive-result")
  public ResponseEntity<Void> receiveResult(
      @PathVariable String idJob, @Valid @RequestBody RecebeRespostaRequest payload) {
    LOGGER.info(
        "[GeraLanding][PresetDesign] Recebida resposta da IA idJob={} experimentId={} stageCode={} openAiJobId={} inputTokens={} outputTokens={} costUsd={} hasError={}",
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

  /** Recebe a resposta da IA para a etapa presetdesign pelo contrato em português. */
  @PostMapping("/internal/geralanding/design-preset/stage-executions/{idJob}/recebe-resposta")
  public ResponseEntity<Void> recebeResposta(
      @PathVariable String idJob, @Valid @RequestBody RecebeRespostaRequest payload) {
    return receiveResult(idJob, payload);
  }

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/experiments/{experimentId}/geralanding/design-preset/stage-executions/{idJob}")
  public ResponseEntity<RecordBackendPresetDesignDetalheDto> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    RecordBackendPresetDesignDetalheDto response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
